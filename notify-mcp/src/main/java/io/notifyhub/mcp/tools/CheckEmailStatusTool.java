package io.notifyhub.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.notifyhub.core.DeliveryReceipt;
import io.notifyhub.core.DeliveryStatus;
import io.notifyhub.core.NotificationTracker;
import io.notifyhub.core.NotifyHub;
import io.notifyhub.core.channel.EmailStatusProvider;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.mcp.util.ToolResultHelper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * MCP tool for checking the delivery status of a previously sent email.
 *
 * <p>When the email was sent through an API provider (SendGrid, Mailgun, etc.),
 * this tool queries the provider's API to get the current status: delivered,
 * opened, bounced, spam complaint, etc.</p>
 *
 * <p>For SMTP-sent emails, returns the original send status (no provider tracking).</p>
 */
public class CheckEmailStatusTool {

    private final NotifyHub notifyHub;

    public CheckEmailStatusTool(NotifyHub notifyHub) {
        this.notifyHub = notifyHub;
    }

    public SyncToolSpecification specification(McpJsonMapper jsonMapper) {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "receipt_id": {
                      "type": "string",
                      "description": "The delivery receipt ID from a previous send_email call"
                    }
                  },
                  "required": ["receipt_id"]
                }
                """;

        Tool tool = Tool.builder()
                .name("check_email_status")
                .description("Check the delivery status of a previously sent email. " +
                        "Returns detailed status (delivered, opened, bounced, spam complaint, etc.) " +
                        "when using an API provider like SendGrid. " +
                        "For SMTP-sent emails, returns the original send status only.")
                .inputSchema(jsonMapper, schema)
                .build();

        return new SyncToolSpecification(tool, (exchange, arguments) -> {
            try {
                return execute(arguments);
            } catch (Exception e) {
                return ToolResultHelper.error(e);
            }
        });
    }

    private CallToolResult execute(Map<String, Object> args) {
        String receiptId = (String) args.get("receipt_id");

        // Check tracker is available
        NotificationTracker tracker = notifyHub.getTracker();
        if (tracker == null) {
            return ToolResultHelper.error("Delivery tracking is not enabled. " +
                    "Set notify.tracking.enabled=true to use email status checking.");
        }

        // Find the original receipt
        Optional<DeliveryReceipt> receiptOpt = tracker.findById(receiptId);
        if (receiptOpt.isEmpty()) {
            return ToolResultHelper.error("No delivery receipt found with ID: " + receiptId);
        }

        DeliveryReceipt receipt = receiptOpt.get();

        // If there's a providerMessageId, query the provider for current status
        if (receipt.getProviderMessageId() != null) {
            Optional<NotificationChannel> channelOpt = notifyHub.getChannel("email");
            if (channelOpt.isPresent() && channelOpt.get() instanceof EmailStatusProvider provider) {
                DeliveryStatus currentStatus = provider.checkStatus(receipt.getProviderMessageId());

                // Record updated status in tracker
                DeliveryReceipt statusUpdate = DeliveryReceipt.builder()
                        .id(receiptId + "-status-" + System.currentTimeMillis())
                        .channelName("email")
                        .recipient(receipt.getRecipient())
                        .status(currentStatus)
                        .providerMessageId(receipt.getProviderMessageId())
                        .build();
                tracker.record(statusUpdate);

                Map<String, Object> data = new LinkedHashMap<>();
                data.put("receipt_id", receiptId);
                data.put("recipient", receipt.getRecipient());
                data.put("original_status", receipt.getStatus().name());
                data.put("current_status", currentStatus.name());
                data.put("provider_message_id", receipt.getProviderMessageId());
                data.put("tracking_supported", true);

                String statusDescription = describeStatus(currentStatus);
                return ToolResultHelper.success(
                        "Email to " + receipt.getRecipient() + ": " + statusDescription, data);
            }
        }

        // SMTP or no provider tracking available
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("receipt_id", receiptId);
        data.put("recipient", receipt.getRecipient());
        data.put("status", receipt.getStatus().name());
        data.put("tracking_supported", false);
        data.put("note", "SMTP does not support delivery tracking. " +
                "Configure SendGrid for detailed status (delivered, opened, bounced).");

        return ToolResultHelper.success(
                "Email to " + receipt.getRecipient() + ": " + receipt.getStatus().name(), data);
    }

    private String describeStatus(DeliveryStatus status) {
        return switch (status) {
            case DELIVERED -> "Delivered to recipient's inbox";
            case OPENED -> "Opened by recipient";
            case CLICKED -> "Recipient clicked a link";
            case BOUNCED -> "Bounced (invalid address or mailbox full)";
            case SPAM_COMPLAINT -> "Marked as spam by recipient";
            case DROPPED -> "Dropped by provider (suppression list)";
            case DEFERRED -> "Temporarily delayed, will retry";
            case SENT -> "Sent, awaiting delivery confirmation";
            case FAILED -> "Failed to send";
            default -> status.name();
        };
    }
}

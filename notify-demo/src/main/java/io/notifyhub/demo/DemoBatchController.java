package io.notifyhub.demo;

import io.notifyhub.core.*;
import io.notifyhub.core.audience.AudienceManager;
import io.notifyhub.core.audience.Contact;
import io.notifyhub.core.audience.ContactRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Batch, multi-channel, audience, and contact management demo endpoints.
 */
@RestController
@Tag(name = "Batch & Audiences", description = "Multi-channel, batch, contacts, and audience notifications")
public class DemoBatchController {

    private final NotifyHub notify;

    @Autowired(required = false)
    private AudienceManager audienceManager;

    @Autowired(required = false)
    private ContactRepository contactRepository;

    public DemoBatchController(NotifyHub notify) {
        this.notify = notify;
    }

    // ===================== MULTI-CHANNEL =====================

    @Operation(summary = "Send multi-channel", description = "Send to Email + Slack simultaneously")
    @PostMapping("/send/multi")
    public Map<String, String> sendMulti(
            @RequestParam(defaultValue = "security@test.com") String email,
            @RequestParam(defaultValue = "#security-alerts") String slackChannel) {

        FakeUser user = new FakeUser("Admin", email, null);

        notify.to(user)
                .via(Channel.EMAIL)
                .via(Channel.SLACK)
                .subject("Security Alert")
                .content("Login detected from a new device: Windows 11, Chrome 120, IP: 189.40.xx.xx")
                .sendAll();

        return Map.of(
                "status", "sent to ALL channels",
                "channels", "email + slack",
                "email_to", email,
                "slack_to", slackChannel,
                "tip", "Check GET /inbox AND GET /inbox/slack — both received!"
        );
    }

    // ===================== CONTACTS =====================

    @Operation(summary = "Create contact", description = "Create a contact with tags for audience segmentation")
    @PostMapping("/contacts")
    public Map<String, Object> createContact(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) List<String> tags) {

        if (contactRepository == null) {
            return Map.of("error", "Audience is not enabled. Set notify.audience.enabled=true");
        }

        Contact.Builder builder = Contact.builder().name(name).email(email);
        if (phone != null) builder.phone(phone);
        if (tags != null) tags.forEach(builder::tag);

        Contact contact = contactRepository.save(builder.build());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "created");
        response.put("contact", Map.of(
                "id", contact.getId(),
                "name", contact.getName(),
                "email", contact.getEmail(),
                "tags", contact.getTags()
        ));
        return response;
    }

    @Operation(summary = "List contacts", description = "List all contacts")
    @GetMapping("/contacts")
    public Map<String, Object> listContacts() {
        if (contactRepository == null) {
            return Map.of("error", "Audience is not enabled. Set notify.audience.enabled=true");
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Contact c : contactRepository.findAll()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("email", c.getEmail());
            m.put("tags", c.getTags());
            list.add(m);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", list.size());
        response.put("contacts", list);
        return response;
    }

    @Operation(summary = "Seed demo contacts", description = "Create sample contacts with tags for testing audiences")
    @PostMapping("/contacts/seed")
    public Map<String, Object> seedContacts() {
        if (contactRepository == null) {
            return Map.of("error", "Audience is not enabled. Set notify.audience.enabled=true");
        }

        contactRepository.save(Contact.builder()
                .name("Alice VIP").email("alice@test.com")
                .tag("vip").tag("plan:premium").tag("region:us-east").build());
        contactRepository.save(Contact.builder()
                .name("Bob Premium").email("bob@test.com")
                .tag("plan:premium").tag("region:us-west").build());
        contactRepository.save(Contact.builder()
                .name("Carol Free").email("carol@test.com")
                .tag("plan:free").tag("region:us-east").build());
        contactRepository.save(Contact.builder()
                .name("Dave VIP").email("dave@test.com")
                .tag("vip").tag("plan:premium").tag("region:eu-west").build());

        return Map.of(
                "status", "seeded",
                "contacts", contactRepository.count(),
                "tip", "Try POST /audiences?name=premium-users&tags=plan:premium to create an audience"
        );
    }

    // ===================== AUDIENCES =====================

    @Operation(summary = "Create audience", description = "Create a named audience with tag filters (AND logic)")
    @PostMapping("/audiences")
    public Map<String, Object> createAudience(
            @RequestParam String name,
            @RequestParam List<String> tags) {

        if (audienceManager == null) {
            return Map.of("error", "Audience is not enabled. Set notify.audience.enabled=true");
        }

        io.notifyhub.core.audience.Audience audience = audienceManager.createAudience(
                name, new LinkedHashSet<>(tags));
        List<Contact> resolved = audienceManager.resolve(audience);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "created");
        response.put("audience", Map.of(
                "name", audience.getName(),
                "tags", audience.getTags(),
                "matchingContacts", resolved.size()
        ));
        return response;
    }

    @Operation(summary = "Send to audience", description = "Send a notification to all contacts in an audience")
    @PostMapping("/send/audience")
    public Map<String, Object> sendToAudience(
            @RequestParam String audience,
            @RequestParam(defaultValue = "Audience Notification") String subject,
            @RequestParam(defaultValue = "Hello from NotifyHub!") String message) {

        if (audienceManager == null) {
            return Map.of("error", "Audience is not enabled. Set notify.audience.enabled=true");
        }

        List<DeliveryReceipt> receipts = notify.toAudience(audience)
                .via(Channel.EMAIL)
                .subject(subject)
                .content(message)
                .send();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "sent to audience");
        response.put("audience", audience);
        response.put("recipientCount", receipts.size());
        response.put("receipts", receipts.stream().map(r -> Map.of(
                "recipient", r.getRecipient(),
                "status", r.getStatus().name()
        )).toList());
        return response;
    }
}

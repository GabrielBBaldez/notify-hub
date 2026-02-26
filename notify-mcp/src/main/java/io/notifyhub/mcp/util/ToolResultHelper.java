package io.notifyhub.mcp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

import java.util.List;
import java.util.Map;

public final class ToolResultHelper {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private ToolResultHelper() {}

    public static CallToolResult success(String message, Map<String, Object> data) {
        try {
            String json = MAPPER.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(Map.of("message", message, "data", data));
            return new CallToolResult(List.of(new TextContent(json)), false);
        } catch (Exception e) {
            return new CallToolResult(List.of(new TextContent(message)), false);
        }
    }

    public static CallToolResult success(String message) {
        return new CallToolResult(List.of(new TextContent(message)), false);
    }

    public static CallToolResult successJson(Object data) {
        try {
            String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(data);
            return new CallToolResult(List.of(new TextContent(json)), false);
        } catch (Exception e) {
            return new CallToolResult(List.of(new TextContent(data.toString())), false);
        }
    }

    public static CallToolResult error(Exception e) {
        String msg = "Error: " + e.getClass().getSimpleName() + " — " + e.getMessage();
        return new CallToolResult(List.of(new TextContent(msg)), true);
    }

    public static CallToolResult error(String message) {
        return new CallToolResult(List.of(new TextContent("Error: " + message)), true);
    }
}

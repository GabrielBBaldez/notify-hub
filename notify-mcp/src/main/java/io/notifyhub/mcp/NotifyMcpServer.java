package io.notifyhub.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.OutputStream;
import java.io.PrintStream;

@SpringBootApplication
public class NotifyMcpServer {

    // Saved before redirect so McpServerRunner can restore it for MCP STDIO transport
    public static final PrintStream ORIGINAL_STDOUT = System.out;

    public static void main(String[] args) {
        // MCP uses stdout for JSON-RPC — redirect System.out during Spring Boot
        // startup to suppress messages (e.g. Commons Logging discovery).
        // McpServerRunner restores it before creating the STDIO transport.
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        SpringApplication app = new SpringApplication(NotifyMcpServer.class);
        app.run(args);
    }
}

package io.notifyhub.mcp;

import io.notifyhub.mcp.setup.SetupWizard;
import io.notifyhub.mcp.setup.UpdateChecker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.OutputStream;
import java.io.PrintStream;

@SpringBootApplication
public class NotifyMcpServer {

    public static final String VERSION = "1.1.0";

    // Saved before redirect so McpServerRunner can restore it for MCP STDIO transport
    public static final PrintStream ORIGINAL_STDOUT = System.out;

    public static void main(String[] args) {
        // ── CLI commands (run BEFORE Spring Boot) ──────────────────
        if (args.length > 0) {
            switch (args[0]) {
                case "--setup" -> { SetupWizard.run(); return; }
                case "--update" -> { UpdateChecker.run(); return; }
                case "--version", "-v" -> { System.out.println("NotifyHub MCP Server v" + VERSION); return; }
                case "--help", "-h" -> { printHelp(); return; }
            }
        }

        // ── Normal MCP server startup ──────────────────────────────
        // MCP uses stdout for JSON-RPC — redirect System.out during Spring Boot
        // startup to suppress messages (e.g. Commons Logging discovery).
        // McpServerRunner restores it before creating the STDIO transport.
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        SpringApplication app = new SpringApplication(NotifyMcpServer.class);
        app.run(args);
    }

    private static void printHelp() {
        System.out.println("NotifyHub MCP Server v" + VERSION);
        System.out.println();
        System.out.println("Usage: notifyhub-mcp [command]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  (none)       Start MCP server (STDIO transport)");
        System.out.println("  --setup      Interactive setup wizard");
        System.out.println("  --update     Check for updates and install");
        System.out.println("  --version    Show version");
        System.out.println("  --help       Show this help");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  notifyhub-mcp --setup     Configure channels & generate config");
        System.out.println("  notifyhub-mcp --update    Update to latest version");
        System.out.println("  notifyhub-mcp             Start server (used by Claude)");
        System.out.println();
        System.out.println("Docs: https://gabrielbbaldez.github.io/notify-hub/docs.html");
    }
}

package io.notifyhub.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.OutputStream;
import java.io.PrintStream;

@SpringBootApplication
public class NotifyMcpServer {

    public static void main(String[] args) {
        // MCP uses stdout for JSON-RPC — redirect System.out during Spring Boot
        // startup to suppress any messages (e.g. Commons Logging discovery),
        // then restore it so the MCP transport can use it.
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        try {
            SpringApplication app = new SpringApplication(NotifyMcpServer.class);
            app.run(args);
        } finally {
            System.setOut(originalOut);
        }
    }
}

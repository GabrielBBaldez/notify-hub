package io.notifyhub.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NotifyMcpServer {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(NotifyMcpServer.class);
        app.run(args);
    }
}

package io.notifyhub.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * NotifyHub Demo Application.
 *
 * Run this and open http://localhost:8080 to see all available endpoints.
 * Uses GreenMail (embedded SMTP server) — no external config needed!
 */
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}

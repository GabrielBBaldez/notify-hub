package io.notifyhub.demo;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notifyHubOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NotifyHub API")
                        .description("Unified notification REST API — send via Email, SMS, WhatsApp, Slack, Telegram, Discord, Teams, Google Chat, and more. One API, every channel.")
                        .version("0.8.0")
                        .contact(new Contact()
                                .name("Gabriel Baldez")
                                .url("https://github.com/GabrielBBaldez/notify-hub"))
                        .license(new License()
                                .name("MIT")
                                .url("https://github.com/GabrielBBaldez/notify-hub/blob/master/LICENSE")));
    }
}

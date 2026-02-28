package io.notifyhub.admin;

import io.notifyhub.core.NotifyHub;
import io.notifyhub.core.StatusWebhookListener;
import io.notifyhub.core.audience.AudienceManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(afterName = "io.notifyhub.spring.NotifyAutoConfiguration")
@ConditionalOnClass(name = "org.thymeleaf.spring6.SpringTemplateEngine")
@ConditionalOnBean(NotifyHub.class)
@ConditionalOnProperty(prefix = "notify.admin", name = "enabled", havingValue = "true")
public class NotifyAdminAutoConfiguration {

    @Bean
    public NotifyAdminController notifyAdminController(NotifyHub hub,
            ObjectProvider<StatusWebhookListener> webhookListenerProvider,
            ObjectProvider<AudienceManager> audienceManagerProvider) {
        return new NotifyAdminController(hub, webhookListenerProvider, audienceManagerProvider);
    }
}

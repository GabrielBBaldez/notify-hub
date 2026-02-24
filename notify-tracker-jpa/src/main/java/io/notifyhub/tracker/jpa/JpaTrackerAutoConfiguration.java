package io.notifyhub.tracker.jpa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Auto-configuration for the JPA-backed notification tracker.
 *
 * <p>Activated when:</p>
 * <ul>
 *   <li>{@code jakarta.persistence.EntityManager} is on the classpath</li>
 *   <li>{@code notify.tracking.type=jpa} is set in application properties</li>
 * </ul>
 *
 * <p>Configures JPA repositories and entity scanning for the tracker entities,
 * and creates a {@link JpaNotificationTracker} bean.</p>
 */
@AutoConfiguration
@ConditionalOnClass(name = "jakarta.persistence.EntityManager")
@ConditionalOnProperty(prefix = "notify.tracking", name = "type", havingValue = "jpa")
@EnableJpaRepositories(basePackageClasses = DeliveryReceiptRepository.class)
@EntityScan(basePackageClasses = DeliveryReceiptEntity.class)
public class JpaTrackerAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JpaTrackerAutoConfiguration.class);

    @Bean
    public JpaNotificationTracker jpaNotificationTracker(DeliveryReceiptRepository repository) {
        log.info("NotifyHub: JPA delivery tracking enabled");
        return new JpaNotificationTracker(repository);
    }
}

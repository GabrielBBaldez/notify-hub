package io.notifyhub.audit.jpa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Auto-configuration for the JPA-backed audit log.
 *
 * <p>Activated when:</p>
 * <ul>
 *   <li>{@code jakarta.persistence.EntityManager} is on the classpath</li>
 *   <li>{@code notify.audit.type=jpa} is set in application properties</li>
 * </ul>
 *
 * <p>Configures JPA repositories and entity scanning for the audit entities,
 * and creates a {@link JpaAuditLog} bean.</p>
 */
@AutoConfiguration
@ConditionalOnClass(name = "jakarta.persistence.EntityManager")
@ConditionalOnProperty(prefix = "notify.audit", name = "type", havingValue = "jpa")
@EnableJpaRepositories(basePackageClasses = AuditEntryRepository.class)
@EntityScan(basePackageClasses = AuditEntryEntity.class)
public class JpaAuditLogAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JpaAuditLogAutoConfiguration.class);

    @Bean
    public JpaAuditLog jpaAuditLog(AuditEntryRepository repository) {
        log.info("NotifyHub: JPA audit log enabled");
        return new JpaAuditLog(repository);
    }
}

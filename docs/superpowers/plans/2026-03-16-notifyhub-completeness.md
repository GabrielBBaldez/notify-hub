# NotifyHub Completeness Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make NotifyHub production-ready with full test coverage, community files, CI enhancements, documentation, and 3 new channels (AWS SNS, Mailgun, PagerDuty).

**Architecture:** Bottom-up approach — fix quality gaps first (tests, CI), then community/docs, then new features. Each workstream produces independently committable work.

**Tech Stack:** Java 17, Maven, Spring Boot 3.2.5, JUnit 5, Mockito 5, JaCoCo, GitHub Actions, AWS SDK v2

**Spec:** `docs/superpowers/specs/2026-03-16-notifyhub-completeness-design.md`

---

## Chunk 1: Tests + JaCoCo

### Task 1: TwilioSmsChannel Tests

**Files:**
- Create: `notify-channels/notify-sms/src/test/java/io/notifyhub/channel/sms/TwilioSmsChannelTest.java`

- [ ] **Step 1: Create TwilioSmsChannelTest**

```java
package io.notifyhub.channel.sms;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TwilioSmsChannelTest {

    private TwilioConfig testConfig() {
        return TwilioConfig.builder()
                .accountSid("ACtest000000000000000000000000000")
                .authToken("test-auth-token")
                .fromNumber("+15551234567")
                .build();
    }

    @Test
    @DisplayName("getName() returns 'sms'")
    void getName() {
        TwilioSmsChannel channel = new TwilioSmsChannel(testConfig());
        assertEquals("sms", channel.getName());
    }

    @Test
    @DisplayName("isAvailable() returns true when config is valid")
    void isAvailable() {
        TwilioSmsChannel channel = new TwilioSmsChannel(testConfig());
        // isAvailable calls ensureInitialized which calls Twilio.init
        // With fake credentials it still returns true (init doesn't validate)
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("send() throws NotificationSendException with invalid credentials")
    void sendFailsWithInvalidCredentials() {
        TwilioSmsChannel channel = new TwilioSmsChannel(testConfig());

        Notification notification = new Notification(
                "+15559876543", "sms", null, null, "Test SMS message", Map.of());

        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }
}
```

- [ ] **Step 2: Run test to verify**

Run: `mvn test -pl notify-channels/notify-sms -Dtest="TwilioSmsChannelTest" -B`
Expected: All 3 tests PASS

- [ ] **Step 3: Commit**

```bash
git add notify-channels/notify-sms/src/test/java/io/notifyhub/channel/sms/TwilioSmsChannelTest.java
git commit -m "test: add TwilioSmsChannel unit tests"
```

### Task 2: TwilioConfig Tests

**Files:**
- Create: `notify-channels/notify-sms/src/test/java/io/notifyhub/channel/sms/TwilioConfigTest.java`

- [ ] **Step 1: Create TwilioConfigTest**

```java
package io.notifyhub.channel.sms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TwilioConfigTest {

    @Test
    @DisplayName("Builder creates config with all fields")
    void builderCreatesConfig() {
        TwilioConfig config = TwilioConfig.builder()
                .accountSid("ACtest000000000000000000000000000")
                .authToken("test-token")
                .fromNumber("+15551234567")
                .build();

        assertEquals("ACtest000000000000000000000000000", config.getAccountSid());
        assertEquals("test-token", config.getAuthToken());
        assertEquals("+15551234567", config.getFromNumber());
    }

    @Test
    @DisplayName("Builder throws on null accountSid")
    void requiresAccountSid() {
        assertThrows(IllegalArgumentException.class, () ->
                TwilioConfig.builder()
                        .authToken("token")
                        .fromNumber("+15551234567")
                        .build());
    }

    @Test
    @DisplayName("Builder throws on blank accountSid")
    void requiresNonBlankAccountSid() {
        assertThrows(IllegalArgumentException.class, () ->
                TwilioConfig.builder()
                        .accountSid("")
                        .authToken("token")
                        .fromNumber("+15551234567")
                        .build());
    }

    @Test
    @DisplayName("Builder throws on null authToken")
    void requiresAuthToken() {
        assertThrows(IllegalArgumentException.class, () ->
                TwilioConfig.builder()
                        .accountSid("ACtest")
                        .fromNumber("+15551234567")
                        .build());
    }

    @Test
    @DisplayName("Builder throws on null fromNumber")
    void requiresFromNumber() {
        assertThrows(IllegalArgumentException.class, () ->
                TwilioConfig.builder()
                        .accountSid("ACtest")
                        .authToken("token")
                        .build());
    }
}
```

- [ ] **Step 2: Run test**

Run: `mvn test -pl notify-channels/notify-sms -Dtest="TwilioConfigTest" -B`
Expected: All 5 tests PASS

- [ ] **Step 3: Commit**

```bash
git add notify-channels/notify-sms/src/test/java/io/notifyhub/channel/sms/TwilioConfigTest.java
git commit -m "test: add TwilioConfig validation tests"
```

### Task 3: JpaNotificationTracker Tests

**Files:**
- Create: `notify-tracker-jpa/src/test/java/io/notifyhub/tracker/jpa/JpaNotificationTrackerTest.java`

- [ ] **Step 1: Create JpaNotificationTrackerTest**

```java
package io.notifyhub.tracker.jpa;

import io.notifyhub.core.DeliveryReceipt;
import io.notifyhub.core.DeliveryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JpaNotificationTrackerTest {

    @Mock
    private DeliveryReceiptRepository repository;

    private JpaNotificationTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new JpaNotificationTracker(repository);
    }

    private DeliveryReceipt sampleReceipt(String id, DeliveryStatus status) {
        return DeliveryReceipt.builder()
                .id(id)
                .channelName("email")
                .recipient("user@example.com")
                .status(status)
                .timestamp(Instant.now())
                .build();
    }

    private DeliveryReceiptEntity sampleEntity(String id, DeliveryStatus status) {
        return DeliveryReceiptEntity.fromDeliveryReceipt(sampleReceipt(id, status));
    }

    @Test
    @DisplayName("record() saves receipt to repository")
    void recordSavesReceipt() {
        DeliveryReceipt receipt = sampleReceipt("r-1", DeliveryStatus.SENT);
        tracker.record(receipt);
        verify(repository).save(any(DeliveryReceiptEntity.class));
    }

    @Test
    @DisplayName("findById() returns receipt when found")
    void findByIdReturnsReceipt() {
        DeliveryReceiptEntity entity = sampleEntity("r-1", DeliveryStatus.SENT);
        when(repository.findById("r-1")).thenReturn(Optional.of(entity));

        Optional<DeliveryReceipt> result = tracker.findById("r-1");
        assertTrue(result.isPresent());
        assertEquals("r-1", result.get().getId());
    }

    @Test
    @DisplayName("findById() returns empty when not found")
    void findByIdReturnsEmpty() {
        when(repository.findById("missing")).thenReturn(Optional.empty());
        assertTrue(tracker.findById("missing").isEmpty());
    }

    @Test
    @DisplayName("findByStatus() returns matching receipts")
    void findByStatusReturnsMatching() {
        DeliveryReceiptEntity entity = sampleEntity("r-1", DeliveryStatus.FAILED);
        when(repository.findByStatusOrderByTimestampDesc(DeliveryStatus.FAILED))
                .thenReturn(List.of(entity));

        List<DeliveryReceipt> results = tracker.findByStatus(DeliveryStatus.FAILED);
        assertEquals(1, results.size());
        assertEquals(DeliveryStatus.FAILED, results.get(0).getStatus());
    }

    @Test
    @DisplayName("findByRecipient() returns matching receipts")
    void findByRecipientReturnsMatching() {
        DeliveryReceiptEntity entity = sampleEntity("r-1", DeliveryStatus.SENT);
        when(repository.findByRecipientOrderByTimestampDesc("user@example.com"))
                .thenReturn(List.of(entity));

        List<DeliveryReceipt> results = tracker.findByRecipient("user@example.com");
        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("count() delegates to repository")
    void countDelegatesToRepository() {
        when(repository.count()).thenReturn(42L);
        assertEquals(42L, tracker.count());
    }

    @Test
    @DisplayName("clear() deletes all from repository")
    void clearDeletesAll() {
        tracker.clear();
        verify(repository).deleteAll();
    }
}
```

- [ ] **Step 2: Run test**

Run: `mvn test -pl notify-tracker-jpa -Dtest="JpaNotificationTrackerTest" -B`
Expected: All 6 tests PASS

- [ ] **Step 3: Commit**

```bash
git add notify-tracker-jpa/src/test/java/io/notifyhub/tracker/jpa/JpaNotificationTrackerTest.java
git commit -m "test: add JpaNotificationTracker unit tests"
```

### Task 4: JpaAuditLog Tests

**Files:**
- Create: `notify-audit-jpa/src/test/java/io/notifyhub/audit/jpa/JpaAuditLogTest.java`

- [ ] **Step 1: Create JpaAuditLogTest**

```java
package io.notifyhub.audit.jpa;

import io.notifyhub.core.AuditEntry;
import io.notifyhub.core.AuditEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JpaAuditLogTest {

    @Mock
    private AuditEntryRepository repository;

    private JpaAuditLog auditLog;

    @BeforeEach
    void setUp() {
        auditLog = new JpaAuditLog(repository);
    }

    private AuditEntry sampleEntry(AuditEventType type) {
        return AuditEntry.builder()
                .id("a-1")
                .eventType(type)
                .channelName("slack")
                .recipient("#general")
                .timestamp(Instant.now())
                .build();
    }

    private AuditEntryEntity sampleEntity(AuditEventType type) {
        return AuditEntryEntity.fromAuditEntry(sampleEntry(type));
    }

    @Test
    @DisplayName("record() saves audit entry to repository")
    void recordSavesEntry() {
        auditLog.record(sampleEntry(AuditEventType.SENT));
        verify(repository).save(any(AuditEntryEntity.class));
    }

    @Test
    @DisplayName("findAll() returns all entries")
    void findAllReturnsEntries() {
        AuditEntryEntity entity = sampleEntity(AuditEventType.SENT);
        when(repository.findAllByOrderByTimestampDesc()).thenReturn(List.of(entity));

        List<AuditEntry> results = auditLog.findAll();
        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("findByEventType() returns matching entries")
    void findByEventTypeReturnsMatching() {
        AuditEntryEntity entity = sampleEntity(AuditEventType.FAILED);
        when(repository.findByEventTypeOrderByTimestampDesc(AuditEventType.FAILED))
                .thenReturn(List.of(entity));

        List<AuditEntry> results = auditLog.findByEventType(AuditEventType.FAILED);
        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("count() delegates to repository")
    void countDelegatesToRepository() {
        when(repository.count()).thenReturn(10L);
        assertEquals(10L, auditLog.count());
    }

    @Test
    @DisplayName("clear() deletes all from repository")
    void clearDeletesAll() {
        auditLog.clear();
        verify(repository).deleteAll();
    }
}
```

- [ ] **Step 2: Run test**

Run: `mvn test -pl notify-audit-jpa -Dtest="JpaAuditLogTest" -B`
Expected: All 5 tests PASS

- [ ] **Step 3: Commit**

```bash
git add notify-audit-jpa/src/test/java/io/notifyhub/audit/jpa/JpaAuditLogTest.java
git commit -m "test: add JpaAuditLog unit tests"
```

### Task 5: JaCoCo Plugin

**Files:**
- Modify: `pom.xml` (root) — add jacoco-maven-plugin to pluginManagement

- [ ] **Step 1: Add JaCoCo plugin to root pom.xml**

Add inside `<build><pluginManagement><plugins>`:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

And add to `<build><plugins>` to activate for all modules:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
</plugin>
```

- [ ] **Step 2: Verify build still works**

Run: `mvn clean verify -B -pl notify-core`
Expected: BUILD SUCCESS with JaCoCo report generated at `notify-core/target/site/jacoco/`

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "build: add JaCoCo code coverage reporting"
```

---

## Chunk 2: Community Files

### Task 6: CONTRIBUTING.md

**Files:**
- Create: `CONTRIBUTING.md`

- [ ] **Step 1: Create CONTRIBUTING.md**

```markdown
# Contributing to NotifyHub

Thank you for your interest in contributing to NotifyHub! This guide will help you get started.

## Prerequisites

- Java 17 or later
- Maven 3.8+
- Git

## Getting Started

1. Fork the repository on GitHub
2. Clone your fork:
   ```bash
   git clone https://github.com/YOUR_USERNAME/notify-hub.git
   cd notify-hub
   ```
3. Build and run tests:
   ```bash
   mvn clean verify -B
   ```

## Project Structure

NotifyHub is a Maven multi-module project:

- `notify-core` — Core API (zero Spring dependency)
- `notify-channels/notify-{name}` — One module per notification channel
- `notify-spring-boot-starter` — Spring Boot auto-configuration
- `notify-mcp` — MCP Server for AI agents
- `notify-demo` — Demo application

## Code Conventions

- **Java 17** features encouraged (records, sealed classes, text blocks, pattern matching)
- **No Lombok** — use manual builders
- **Logging**: SLF4J with `private static final Logger log = LoggerFactory.getLogger(Foo.class)`
- **Immutability**: final fields, `Collections.unmodifiable*()`, copy methods
- **Thread safety**: all channel implementations must be stateless and thread-safe
- **Tests**: JUnit 5 + Mockito, `@DisplayName` on every test method

## Creating a New Channel

1. Create module `notify-channels/notify-{name}/` with `pom.xml` depending on `notify-core`
2. Create `{Name}Config` with builder pattern and validation in `build()`
3. Create `{Name}Channel` implementing `NotificationChannel`:
   - `getName()` returns hyphenated name (e.g., `"google-chat"`)
   - `isAvailable()` checks config validity
   - `send(Notification)` performs the actual send
4. Add module to root `pom.xml` `<modules>` section
5. Add `Notify{Name}AutoConfiguration` in `notify-spring-boot-starter`
6. Add properties to `NotifyProperties` inner classes
7. Write unit tests with `@DisplayName` annotations
8. (Optional) Add `Send{Name}Tool` in `notify-mcp`

## Submitting Changes

1. Create a feature branch: `git checkout -b feat/my-feature`
2. Make your changes with clear, focused commits
3. Ensure all tests pass: `mvn clean verify -B`
4. Push and open a Pull Request against `master`

### PR Checklist

- [ ] Tests added/updated for new functionality
- [ ] All tests pass (`mvn clean verify -B`)
- [ ] Code follows project conventions (no Lombok, manual builders, @DisplayName)
- [ ] CHANGELOG.md updated (if applicable)
- [ ] Documentation updated (if applicable)

## Reporting Issues

- **Bugs**: Use the [Bug Report](.github/ISSUE_TEMPLATE/bug_report.md) template
- **Features**: Use the [Feature Request](.github/ISSUE_TEMPLATE/feature_request.md) template
- **New Channels**: Use the [New Channel](.github/ISSUE_TEMPLATE/new_channel.md) template
- **Security**: See [SECURITY.md](SECURITY.md) for vulnerability reporting

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
```

- [ ] **Step 2: Commit**

```bash
git add CONTRIBUTING.md
git commit -m "docs: add CONTRIBUTING.md"
```

### Task 7: CHANGELOG.md

**Files:**
- Create: `CHANGELOG.md`

- [ ] **Step 1: Create CHANGELOG.md**

```markdown
# Changelog

All notable changes to NotifyHub will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/), and this project adheres to [Semantic Versioning](https://semver.org/).

## [1.0.0] - 2026-03-16

### Added
- 23 notification channels: Email (SMTP), SMS (Twilio), WhatsApp (Cloud API + Twilio), Slack, Telegram, Discord, Microsoft Teams, Firebase Push (FCM v1), Webhook, WebSocket, Google Chat, Twitter/X, LinkedIn, Notion, Twitch, YouTube, Instagram, SendGrid, TikTok Shop, Facebook, AWS SNS, Mailgun, PagerDuty
- Fluent API: `.to().via().content().send()` with builder pattern
- MCP Server with 36 tools for AI agent integration (STDIO transport)
- Spring Boot 3.x auto-configuration with per-channel activation via `@ConditionalOnProperty`
- Retry policies: none, fixed, exponential backoff
- Token bucket rate limiting with per-channel presets
- Delivery tracking with `DeliveryReceipt` and status lifecycle
- Dead letter queue for failed notifications after retry exhaustion
- Template engine (Mustache) with i18n locale support
- Multi-channel send with automatic fallback
- Scheduled notifications with cancel/list support
- Contact and audience management with tag-based segmentation
- Batch notifications to audiences
- Priority levels (URGENT bypasses rate limits, HIGH, NORMAL, LOW)
- JPA delivery tracking (`notify-tracker-jpa`)
- JPA audit logging (`notify-audit-jpa`)
- RabbitMQ and Kafka queue integrations
- Web admin dashboard (Thymeleaf)
- Micrometer metrics and OpenTelemetry tracing (optional)
- Actuator health indicator and info contributor
- Demo application with 34 REST endpoints

### Security
- HMAC-SHA256 webhook signature verification
- OAuth2 JWT assertion flow for Firebase Cloud Messaging
- Facebook access token moved from URL to Authorization header
```

- [ ] **Step 2: Commit**

```bash
git add CHANGELOG.md
git commit -m "docs: add CHANGELOG.md for 1.0.0 release"
```

### Task 8: CODE_OF_CONDUCT.md

**Files:**
- Create: `CODE_OF_CONDUCT.md`

- [ ] **Step 1: Create CODE_OF_CONDUCT.md**

Standard Contributor Covenant v2.1. Contact email: the repo owner's GitHub email.

- [ ] **Step 2: Commit**

```bash
git add CODE_OF_CONDUCT.md
git commit -m "docs: add Contributor Covenant Code of Conduct"
```

### Task 9: SECURITY.md

**Files:**
- Create: `SECURITY.md`

- [ ] **Step 1: Create SECURITY.md**

```markdown
# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in NotifyHub, please report it responsibly.

**Do NOT open a public GitHub issue for security vulnerabilities.**

Instead, please email: **gabrielbbaldez@gmail.com**

Include:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

## Response Timeline

- **Acknowledgment**: Within 48 hours
- **Assessment**: Within 1 week
- **Fix**: Depends on severity (critical: ASAP, high: 2 weeks, medium: next release)

## Supported Versions

| Version | Supported |
|---------|-----------|
| 1.0.x   | Yes       |
| < 1.0   | No        |

## Scope

This policy covers the NotifyHub library code. Third-party APIs and services (Twilio, Discord, Slack, etc.) have their own security policies.
```

- [ ] **Step 2: Commit**

```bash
git add SECURITY.md
git commit -m "docs: add SECURITY.md for vulnerability reporting"
```

### Task 10: GitHub Issue & PR Templates

**Files:**
- Create: `.github/ISSUE_TEMPLATE/bug_report.md`
- Create: `.github/ISSUE_TEMPLATE/feature_request.md`
- Create: `.github/ISSUE_TEMPLATE/new_channel.md`
- Create: `.github/pull_request_template.md`

- [ ] **Step 1: Create bug_report.md**

```markdown
---
name: Bug Report
about: Report a bug in NotifyHub
labels: bug
---

## Description
A clear description of the bug.

## Steps to Reproduce
1. Configure channel with...
2. Call `notifyHub.to(...).via(...).send()`
3. See error...

## Expected Behavior
What you expected to happen.

## Actual Behavior
What actually happened. Include stack traces if applicable.

## Environment
- NotifyHub version:
- Java version:
- Spring Boot version (if applicable):
- Channel:
- OS:
```

- [ ] **Step 2: Create feature_request.md**

```markdown
---
name: Feature Request
about: Suggest a new feature for NotifyHub
labels: enhancement
---

## Description
A clear description of the feature you'd like.

## Use Case
Why do you need this feature? What problem does it solve?

## Proposed Solution
How you think it should work (API examples, configuration, etc.)

## Alternatives Considered
Any alternative solutions you've considered.
```

- [ ] **Step 3: Create new_channel.md**

```markdown
---
name: New Channel Request
about: Suggest a new notification channel
labels: new-channel
---

## Channel Name
e.g., PagerDuty, AWS SNS, Mailgun

## Provider API Documentation
Link to the official API docs.

## Authentication
How does the API authenticate? (API key, OAuth2, webhook URL, etc.)

## Use Case
Why should NotifyHub support this channel?

## Willingness to Contribute
- [ ] I'd like to implement this channel myself
- [ ] I'd like help implementing this channel
- [ ] I'm just suggesting the idea
```

- [ ] **Step 4: Create pull_request_template.md**

```markdown
## Summary
Brief description of changes.

## Related Issue
Closes #(issue number)

## Checklist
- [ ] Tests added/updated
- [ ] All tests pass (`mvn clean verify -B`)
- [ ] Code follows project conventions
- [ ] CHANGELOG.md updated (if applicable)
- [ ] Documentation updated (if applicable)

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] New channel
- [ ] Documentation
- [ ] CI/CD
```

- [ ] **Step 5: Commit**

```bash
git add .github/ISSUE_TEMPLATE/ .github/pull_request_template.md
git commit -m "docs: add GitHub issue and PR templates"
```

---

## Chunk 3: CI/CD Enhancements

### Task 11: Dependabot Configuration

**Files:**
- Create: `.github/dependabot.yml`

- [ ] **Step 1: Create dependabot.yml**

```yaml
version: 2
updates:
  - package-ecosystem: "maven"
    directory: "/"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 10
    labels:
      - "dependencies"

  - package-ecosystem: "github-actions"
    directory: "/"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 5
    labels:
      - "dependencies"
      - "ci"
```

- [ ] **Step 2: Commit**

```bash
git add .github/dependabot.yml
git commit -m "ci: add Dependabot for Maven and GitHub Actions"
```

### Task 12: CodeQL Workflow

**Files:**
- Create: `.github/workflows/codeql.yml`

- [ ] **Step 1: Create codeql.yml**

```yaml
name: CodeQL

on:
  push:
    branches: [ master ]
  pull_request:
    branches: [ master ]
  schedule:
    - cron: '0 6 * * 1'

jobs:
  analyze:
    name: Analyze
    runs-on: ubuntu-latest
    permissions:
      security-events: write

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Initialize CodeQL
        uses: github/codeql-action/init@v3
        with:
          languages: java

      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Build
        run: mvn compile -B -DskipTests

      - name: Perform CodeQL Analysis
        uses: github/codeql-action/analyze@v3
```

- [ ] **Step 2: Commit**

```bash
git add .github/workflows/codeql.yml
git commit -m "ci: add CodeQL security analysis workflow"
```

### Task 13: CI Coverage Upload

**Files:**
- Modify: `.github/workflows/ci.yml` — add JaCoCo report upload

- [ ] **Step 1: Add Codecov upload step to ci.yml**

After the `mvn clean verify -B` step, add:

```yaml
      - name: Upload coverage to Codecov
        if: matrix.java == '17'
        uses: codecov/codecov-action@v4
        with:
          files: '**/target/site/jacoco/jacoco.xml'
          fail_ci_if_error: false
        env:
          CODECOV_TOKEN: ${{ secrets.CODECOV_TOKEN }}
```

- [ ] **Step 2: Add badges to README.md**

After the existing badges line, add:

```markdown
[![codecov](https://codecov.io/gh/GabrielBBaldez/notify-hub/graph/badge.svg)](https://codecov.io/gh/GabrielBBaldez/notify-hub)
```

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/ci.yml README.md
git commit -m "ci: add Codecov coverage upload and badge"
```

---

## Chunk 4: Documentation + Version Sync

### Task 14: Version Sync

**Files:**
- Modify: `CLAUDE.md` — update version references from 0.9.0 to 1.0.0

- [ ] **Step 1: Update CLAUDE.md version**

Replace all occurrences of `0.9.0` with `1.0.0`:
- `**Version**: 0.9.0` → `**Version**: 1.0.0`
- `git tag v0.9.0` → `git tag v1.0.0`
- `git push origin v0.9.0` → `git push origin v1.0.0`
- `notify-mcp-0.9.0.jar` → `notify-mcp-1.0.0.jar`
- `**Build**: Maven multi-module (27 modules)` → update module count after new channels are added

- [ ] **Step 2: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: sync CLAUDE.md version to 1.0.0"
```

### Task 15: Architecture Diagram in docs.html

**Files:**
- Modify: `docs/docs.html` — add Architecture section with inline SVG

- [ ] **Step 1: Add Architecture section**

Add a new section before the Channels section in docs.html with an inline SVG diagram showing:
- App → NotifyHub (central box)
- NotifyHub → Channel Router → [Email, SMS, Slack, ...] (fan out)
- Retry loop arrow back to Channel Router
- Rate Limiter gate before channels
- Dead Letter Queue branch on failure
- Fallback arrow to alternative channel

Style with existing CSS variables. Use `<svg>` with `<rect>`, `<text>`, `<line>`, `<marker>` elements.

- [ ] **Step 2: Verify with preview server**

Reload `http://localhost:8091/docs.html` and verify the diagram renders correctly.

- [ ] **Step 3: Commit**

```bash
git add docs/docs.html
git commit -m "docs: add architecture diagram to documentation"
```

### Task 16: Quick Start Guides

**Files:**
- Modify: `docs/docs.html` — add 4 quick start guides

- [ ] **Step 1: Add quick start guides**

Add after the Getting Started section in docs.html:

1. **"Send your first email"** — 5-line Java example with SmtpConfig + NotifyHub
2. **"Multi-channel with fallback"** — email primary, Slack fallback using `.fallback(Channel.SLACK)`
3. **"Scheduled notifications"** — `.schedule(Instant)` + `.cancel()` example
4. **"Audience broadcasting"** — Contact creation, audience with tags, `.sendToAudience()`

Each guide uses the existing `<div class="gs-box">` pattern with code blocks.

- [ ] **Step 2: Verify with preview server**

- [ ] **Step 3: Commit**

```bash
git add docs/docs.html
git commit -m "docs: add quick start guides for common use cases"
```

### Task 17: Custom Channel Guide

**Files:**
- Modify: `docs/docs.html` — add custom channel creation guide

- [ ] **Step 1: Add custom channel guide**

Add a "Creating Custom Channels" section to docs.html with a complete PagerDuty example:
- `PagerDutyConfig` class with builder (routingKey, severity)
- `PagerDutyChannel` class implementing `NotificationChannel`
- Unit test example
- Spring Boot auto-configuration example

- [ ] **Step 2: Commit**

```bash
git add docs/docs.html
git commit -m "docs: add custom channel creation guide with PagerDuty example"
```

### Task 18: i18n Documentation

**Files:**
- Modify: `docs/docs.html` — add i18n section

- [ ] **Step 1: Add i18n documentation**

Add to the Templates section showing:
- `.locale(Locale.forLanguageTag("pt-BR"))` usage
- Template file naming: `templates/notify/welcome/pt-BR.mustache`
- Fallback behavior (falls back to default template if locale not found)

- [ ] **Step 2: Commit**

```bash
git add docs/docs.html
git commit -m "docs: add i18n template documentation"
```

---

## Chunk 5: New Channel — AWS SNS

### Task 19: AWS SNS Module Setup

**Files:**
- Create: `notify-channels/notify-aws-sns/pom.xml`
- Modify: `pom.xml` (root) — add AWS BOM + module

- [ ] **Step 1: Add AWS SDK BOM to root pom.xml**

Add to `<dependencyManagement><dependencies>`:

```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>bom</artifactId>
    <version>2.25.0</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

Add to `<modules>`:

```xml
<module>notify-channels/notify-aws-sns</module>
```

- [ ] **Step 2: Create notify-aws-sns/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.github.gabrielbbaldez</groupId>
        <artifactId>notify-hub</artifactId>
        <version>1.0.0</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>notify-aws-sns</artifactId>
    <name>NotifyHub :: AWS SNS Channel</name>
    <description>Amazon SNS notification channel for NotifyHub</description>

    <dependencies>
        <dependency>
            <groupId>io.github.gabrielbbaldez</groupId>
            <artifactId>notify-core</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>sns</artifactId>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: Commit**

```bash
git add pom.xml notify-channels/notify-aws-sns/pom.xml
git commit -m "build: add notify-aws-sns module skeleton"
```

### Task 20: AWS SNS Config + Channel

**Files:**
- Create: `notify-channels/notify-aws-sns/src/main/java/io/notifyhub/channel/sns/AwsSnsConfig.java`
- Create: `notify-channels/notify-aws-sns/src/main/java/io/notifyhub/channel/sns/AwsSnsChannel.java`

- [ ] **Step 1: Create AwsSnsConfig**

```java
package io.notifyhub.channel.sns;

public class AwsSnsConfig {

    private final String region;
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String topicArn;

    private AwsSnsConfig(Builder builder) {
        this.region = requireNonBlank(builder.region, "AWS region");
        this.accessKeyId = requireNonBlank(builder.accessKeyId, "AWS access key ID");
        this.secretAccessKey = requireNonBlank(builder.secretAccessKey, "AWS secret access key");
        this.topicArn = builder.topicArn; // optional
    }

    public String getRegion() { return region; }
    public String getAccessKeyId() { return accessKeyId; }
    public String getSecretAccessKey() { return secretAccessKey; }
    public String getTopicArn() { return topicArn; }

    public static Builder builder() { return new Builder(); }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be null or blank");
        }
        return value;
    }

    public static class Builder {
        private String region;
        private String accessKeyId;
        private String secretAccessKey;
        private String topicArn;

        public Builder region(String region) { this.region = region; return this; }
        public Builder accessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; return this; }
        public Builder secretAccessKey(String secretAccessKey) { this.secretAccessKey = secretAccessKey; return this; }
        public Builder topicArn(String topicArn) { this.topicArn = topicArn; return this; }

        public AwsSnsConfig build() { return new AwsSnsConfig(this); }
    }
}
```

- [ ] **Step 2: Create AwsSnsChannel**

```java
package io.notifyhub.channel.sns;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

public class AwsSnsChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(AwsSnsChannel.class);

    private final AwsSnsConfig config;
    private final SnsClient snsClient;

    public AwsSnsChannel(AwsSnsConfig config) {
        this.config = config;
        this.snsClient = SnsClient.builder()
                .region(Region.of(config.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.getAccessKeyId(), config.getSecretAccessKey())))
                .build();
    }

    // Package-private constructor for testing with mock client
    AwsSnsChannel(AwsSnsConfig config, SnsClient snsClient) {
        this.config = config;
        this.snsClient = snsClient;
    }

    @Override
    public String getName() {
        return "aws-sns";
    }

    @Override
    public void send(Notification notification) {
        try {
            String content = notification.getRenderedContent();
            String target = notification.getRecipient();

            PublishRequest.Builder requestBuilder = PublishRequest.builder()
                    .message(content);

            if (notification.getSubject() != null) {
                requestBuilder.subject(notification.getSubject());
            }

            // If recipient looks like an ARN, publish directly; otherwise use topic ARN
            if (target != null && target.startsWith("arn:")) {
                requestBuilder.targetArn(target);
            } else if (config.getTopicArn() != null) {
                requestBuilder.topicArn(config.getTopicArn());
            } else if (target != null && target.startsWith("+")) {
                // Phone number — direct SMS via SNS
                requestBuilder.phoneNumber(target);
            } else {
                throw new NotificationSendException("aws-sns",
                        "No valid target: recipient must be an ARN, phone number, or topicArn must be configured");
            }

            PublishResponse response = snsClient.publish(requestBuilder.build());
            log.debug("AWS SNS message published, messageId: {}", response.messageId());

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("aws-sns",
                    "Failed to publish to AWS SNS: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return config.getAccessKeyId() != null && !config.getAccessKeyId().isBlank();
    }
}
```

- [ ] **Step 3: Compile**

Run: `mvn compile -pl notify-channels/notify-aws-sns -am -B -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add notify-channels/notify-aws-sns/src/
git commit -m "feat: add AWS SNS notification channel"
```

### Task 21: AWS SNS Tests

**Files:**
- Create: `notify-channels/notify-aws-sns/src/test/java/io/notifyhub/channel/sns/AwsSnsChannelTest.java`
- Create: `notify-channels/notify-aws-sns/src/test/java/io/notifyhub/channel/sns/AwsSnsConfigTest.java`

- [ ] **Step 1: Create AwsSnsConfigTest**

```java
package io.notifyhub.channel.sns;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AwsSnsConfigTest {

    @Test
    @DisplayName("Builder creates config with all fields")
    void builderCreatesConfig() {
        AwsSnsConfig config = AwsSnsConfig.builder()
                .region("us-east-1")
                .accessKeyId("AKIAIOSFODNN7EXAMPLE")
                .secretAccessKey("secret")
                .topicArn("arn:aws:sns:us-east-1:123456789:my-topic")
                .build();

        assertEquals("us-east-1", config.getRegion());
        assertEquals("AKIAIOSFODNN7EXAMPLE", config.getAccessKeyId());
        assertEquals("secret", config.getSecretAccessKey());
        assertEquals("arn:aws:sns:us-east-1:123456789:my-topic", config.getTopicArn());
    }

    @Test
    @DisplayName("Builder allows null topicArn (optional)")
    void topicArnIsOptional() {
        AwsSnsConfig config = AwsSnsConfig.builder()
                .region("us-east-1")
                .accessKeyId("AKIAIOSFODNN7EXAMPLE")
                .secretAccessKey("secret")
                .build();

        assertNull(config.getTopicArn());
    }

    @Test
    @DisplayName("Builder throws on null region")
    void requiresRegion() {
        assertThrows(IllegalArgumentException.class, () ->
                AwsSnsConfig.builder()
                        .accessKeyId("key")
                        .secretAccessKey("secret")
                        .build());
    }

    @Test
    @DisplayName("Builder throws on blank accessKeyId")
    void requiresAccessKeyId() {
        assertThrows(IllegalArgumentException.class, () ->
                AwsSnsConfig.builder()
                        .region("us-east-1")
                        .accessKeyId("")
                        .secretAccessKey("secret")
                        .build());
    }

    @Test
    @DisplayName("Builder throws on null secretAccessKey")
    void requiresSecretAccessKey() {
        assertThrows(IllegalArgumentException.class, () ->
                AwsSnsConfig.builder()
                        .region("us-east-1")
                        .accessKeyId("key")
                        .build());
    }
}
```

- [ ] **Step 2: Create AwsSnsChannelTest**

```java
package io.notifyhub.channel.sns;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AwsSnsChannelTest {

    @Mock
    private SnsClient snsClient;

    private AwsSnsConfig config;
    private AwsSnsChannel channel;

    @BeforeEach
    void setUp() {
        config = AwsSnsConfig.builder()
                .region("us-east-1")
                .accessKeyId("AKIAIOSFODNN7EXAMPLE")
                .secretAccessKey("secret")
                .topicArn("arn:aws:sns:us-east-1:123456789:my-topic")
                .build();
        channel = new AwsSnsChannel(config, snsClient);
    }

    @Test
    @DisplayName("getName() returns 'aws-sns'")
    void getName() {
        assertEquals("aws-sns", channel.getName());
    }

    @Test
    @DisplayName("isAvailable() returns true when credentials are set")
    void isAvailable() {
        assertTrue(channel.isAvailable());
    }

    @Test
    @DisplayName("send() publishes to topic ARN when recipient is not an ARN")
    void sendPublishesToTopic() {
        when(snsClient.publish(any(PublishRequest.class)))
                .thenReturn(PublishResponse.builder().messageId("msg-123").build());

        Notification notification = new Notification(
                "user@example.com", "aws-sns", "Alert", null, "Server is down", Map.of());

        channel.send(notification);

        verify(snsClient).publish(any(PublishRequest.class));
    }

    @Test
    @DisplayName("send() publishes directly when recipient is an ARN")
    void sendPublishesToArn() {
        when(snsClient.publish(any(PublishRequest.class)))
                .thenReturn(PublishResponse.builder().messageId("msg-456").build());

        Notification notification = new Notification(
                "arn:aws:sns:us-east-1:123:endpoint/abc", "aws-sns", null, null, "Test", Map.of());

        channel.send(notification);

        verify(snsClient).publish(any(PublishRequest.class));
    }

    @Test
    @DisplayName("send() throws when no topic and recipient is not ARN or phone")
    void sendFailsWithoutTarget() {
        AwsSnsConfig noTopicConfig = AwsSnsConfig.builder()
                .region("us-east-1")
                .accessKeyId("key")
                .secretAccessKey("secret")
                .build();
        AwsSnsChannel noTopicChannel = new AwsSnsChannel(noTopicConfig, snsClient);

        Notification notification = new Notification(
                "user@example.com", "aws-sns", null, null, "Test", Map.of());

        assertThrows(NotificationSendException.class, () -> noTopicChannel.send(notification));
    }
}
```

- [ ] **Step 3: Run tests**

Run: `mvn test -pl notify-channels/notify-aws-sns -B`
Expected: All tests PASS

- [ ] **Step 4: Commit**

```bash
git add notify-channels/notify-aws-sns/src/test/
git commit -m "test: add AWS SNS channel and config tests"
```

### Task 22: AWS SNS Spring + MCP + Enum

**Files:**
- Modify: `notify-core/src/main/java/io/notifyhub/core/Channel.java` — add AWS_SNS
- Create: `notify-spring-boot-starter/src/main/java/io/notifyhub/spring/NotifyAwsSnsAutoConfiguration.java`
- Modify: `notify-spring-boot-starter/src/main/java/io/notifyhub/spring/NotifyProperties.java` — add AwsSns inner class
- Modify: `notify-spring-boot-starter/src/main/java/io/notifyhub/spring/NotifyAutoConfiguration.java` — add import
- Modify: `notify-spring-boot-starter/pom.xml` — add notify-aws-sns optional dependency
- Create: `notify-mcp/src/main/java/io/notifyhub/mcp/tools/SendAwsSnsTool.java`
- Modify: `notify-mcp/src/main/java/io/notifyhub/mcp/McpServerRunner.java` — register tool
- Modify: `notify-mcp/pom.xml` — add notify-aws-sns dependency

- [ ] **Step 1: Add AWS_SNS to Channel enum**

Add `AWS_SNS` after `FACEBOOK` in `Channel.java`.

- [ ] **Step 2: Add Spring auto-config**

Create `NotifyAwsSnsAutoConfiguration.java` following Discord pattern:
- `@ConditionalOnClass(name = "io.notifyhub.channel.sns.AwsSnsChannel")`
- `@ConditionalOnProperty(prefix = "notify.channels.aws-sns", name = "access-key-id")`
- Build `AwsSnsConfig` from `NotifyProperties.AwsSns`

- [ ] **Step 3: Add properties class**

Add `AwsSns` inner class to `NotifyProperties.Channels`:
- fields: region, accessKeyId, secretAccessKey, topicArn
- getter/setter for each

- [ ] **Step 4: Add import to NotifyAutoConfiguration**

Add `NotifyAwsSnsAutoConfiguration.class` to the `@Import` list.

- [ ] **Step 5: Add optional dependency to spring-boot-starter pom.xml**

```xml
<dependency>
    <groupId>io.github.gabrielbbaldez</groupId>
    <artifactId>notify-aws-sns</artifactId>
    <version>${project.version}</version>
    <optional>true</optional>
</dependency>
```

- [ ] **Step 6: Create SendAwsSnsTool**

Following SendDiscordTool pattern:
- Tool name: `send_aws_sns`
- Parameters: recipient, body, template, params, subject
- Uses `Channel.AWS_SNS`

- [ ] **Step 7: Register tool in McpServerRunner**

Add `server.addTool(new SendAwsSnsTool(notifyHub).specification(jsonMapper));`

- [ ] **Step 8: Add dependency to MCP pom.xml**

- [ ] **Step 9: Compile and test**

Run: `mvn compile -pl notify-spring-boot-starter,notify-mcp -am -B -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 10: Commit**

```bash
git add notify-core/src/main/java/io/notifyhub/core/Channel.java \
  notify-spring-boot-starter/src/ notify-spring-boot-starter/pom.xml \
  notify-mcp/src/ notify-mcp/pom.xml
git commit -m "feat: integrate AWS SNS with Spring Boot, MCP, and Channel enum"
```

---

## Chunk 6: New Channel — Mailgun

### Task 23: Mailgun Module + Config + Channel

**Files:**
- Create: `notify-channels/notify-mailgun/pom.xml`
- Create: `notify-channels/notify-mailgun/src/main/java/io/notifyhub/channel/mailgun/MailgunConfig.java`
- Create: `notify-channels/notify-mailgun/src/main/java/io/notifyhub/channel/mailgun/MailgunChannel.java`
- Modify: `pom.xml` (root) — add module

- [ ] **Step 1: Add module to root pom.xml**

Add `<module>notify-channels/notify-mailgun</module>` to `<modules>`.

- [ ] **Step 2: Create pom.xml**

Same pattern as notify-discord pom.xml — depends only on notify-core, JUnit, Mockito.

- [ ] **Step 3: Create MailgunConfig**

```java
package io.notifyhub.channel.mailgun;

public class MailgunConfig {

    private final String apiKey;
    private final String domain;
    private final String from;
    private final String region; // "US" or "EU"

    private MailgunConfig(Builder builder) {
        this.apiKey = requireNonBlank(builder.apiKey, "Mailgun API key");
        this.domain = requireNonBlank(builder.domain, "Mailgun domain");
        this.from = requireNonBlank(builder.from, "From address");
        this.region = builder.region != null ? builder.region : "US";
    }

    public String getApiKey() { return apiKey; }
    public String getDomain() { return domain; }
    public String getFrom() { return from; }
    public String getRegion() { return region; }

    public String getBaseUrl() {
        return "EU".equalsIgnoreCase(region)
                ? "https://api.eu.mailgun.net/v3"
                : "https://api.mailgun.net/v3";
    }

    public static Builder builder() { return new Builder(); }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be null or blank");
        }
        return value;
    }

    public static class Builder {
        private String apiKey;
        private String domain;
        private String from;
        private String region;

        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }
        public Builder domain(String domain) { this.domain = domain; return this; }
        public Builder from(String from) { this.from = from; return this; }
        public Builder region(String region) { this.region = region; return this; }

        public MailgunConfig build() { return new MailgunConfig(this); }
    }
}
```

- [ ] **Step 4: Create MailgunChannel**

```java
package io.notifyhub.channel.mailgun;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class MailgunChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(MailgunChannel.class);

    private final MailgunConfig config;
    private final HttpClient httpClient;

    public MailgunChannel(MailgunConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
    }

    MailgunChannel(MailgunConfig config, HttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    @Override
    public String getName() {
        return "mailgun";
    }

    @Override
    public void send(Notification notification) {
        try {
            String url = config.getBaseUrl() + "/" + config.getDomain() + "/messages";
            String content = notification.getRenderedContent();
            String subject = notification.getSubject() != null ? notification.getSubject() : "Notification";

            String formData = "from=" + encode(config.getFrom())
                    + "&to=" + encode(notification.getRecipient())
                    + "&subject=" + encode(subject)
                    + "&text=" + encode(content);

            String auth = Base64.getEncoder().encodeToString(
                    ("api:" + config.getApiKey()).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new NotificationSendException("mailgun",
                        "Mailgun API returned " + response.statusCode() + ": " + response.body());
            }

            log.debug("Mailgun email sent to '{}'", notification.getRecipient());

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("mailgun",
                    "Failed to send via Mailgun: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return config.getApiKey() != null && !config.getApiKey().isBlank();
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
```

- [ ] **Step 5: Compile**

Run: `mvn compile -pl notify-channels/notify-mailgun -am -B -DskipTests`

- [ ] **Step 6: Commit**

```bash
git add pom.xml notify-channels/notify-mailgun/
git commit -m "feat: add Mailgun email notification channel"
```

### Task 24: Mailgun Tests + Spring + MCP

**Files:**
- Create: `notify-channels/notify-mailgun/src/test/java/io/notifyhub/channel/mailgun/MailgunConfigTest.java`
- Create: `notify-channels/notify-mailgun/src/test/java/io/notifyhub/channel/mailgun/MailgunChannelTest.java`
- Modify: `Channel.java` — add MAILGUN
- Create: `NotifyMailgunAutoConfiguration.java`
- Modify: `NotifyProperties.java` — add Mailgun inner class
- Modify: `NotifyAutoConfiguration.java` — add import
- Create: `SendMailgunTool.java`
- Modify: `McpServerRunner.java` — register tool

- [ ] **Step 1: Create MailgunConfigTest**

```java
package io.notifyhub.channel.mailgun;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MailgunConfigTest {

    @Test
    @DisplayName("Builder creates config with all fields")
    void builderCreatesConfig() {
        MailgunConfig config = MailgunConfig.builder()
                .apiKey("key-abc123")
                .domain("mg.example.com")
                .from("noreply@example.com")
                .region("EU")
                .build();

        assertEquals("key-abc123", config.getApiKey());
        assertEquals("mg.example.com", config.getDomain());
        assertEquals("noreply@example.com", config.getFrom());
        assertEquals("EU", config.getRegion());
        assertTrue(config.getBaseUrl().contains("api.eu.mailgun.net"));
    }

    @Test
    @DisplayName("Default region is US")
    void defaultRegionIsUs() {
        MailgunConfig config = MailgunConfig.builder()
                .apiKey("key").domain("mg.example.com").from("a@b.com").build();
        assertEquals("US", config.getRegion());
        assertTrue(config.getBaseUrl().contains("api.mailgun.net"));
    }

    @Test
    @DisplayName("Builder throws on null apiKey")
    void requiresApiKey() {
        assertThrows(IllegalArgumentException.class, () ->
                MailgunConfig.builder().domain("mg.example.com").from("a@b.com").build());
    }

    @Test
    @DisplayName("Builder throws on null domain")
    void requiresDomain() {
        assertThrows(IllegalArgumentException.class, () ->
                MailgunConfig.builder().apiKey("key").from("a@b.com").build());
    }

    @Test
    @DisplayName("Builder throws on blank from")
    void requiresFrom() {
        assertThrows(IllegalArgumentException.class, () ->
                MailgunConfig.builder().apiKey("key").domain("mg.example.com").from("").build());
    }
}
```

- [ ] **Step 2: Create MailgunChannelTest**

```java
package io.notifyhub.channel.mailgun;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MailgunChannelTest {

    private MailgunConfig testConfig() {
        return MailgunConfig.builder()
                .apiKey("key-test123")
                .domain("mg.example.com")
                .from("noreply@example.com")
                .build();
    }

    @Test
    @DisplayName("getName() returns 'mailgun'")
    void getName() {
        assertEquals("mailgun", new MailgunChannel(testConfig()).getName());
    }

    @Test
    @DisplayName("isAvailable() returns true when API key is set")
    void isAvailable() {
        assertTrue(new MailgunChannel(testConfig()).isAvailable());
    }

    @Test
    @DisplayName("send() throws NotificationSendException on invalid API endpoint")
    void sendFailsOnInvalidEndpoint() {
        MailgunChannel channel = new MailgunChannel(testConfig());
        Notification notification = new Notification(
                "user@example.com", "mailgun", "Test Subject", null, "Test body", Map.of());
        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }
}
```

- [ ] **Step 3: Add MAILGUN to Channel enum**

Add `MAILGUN` after `PAGERDUTY` (or after `AWS_SNS`) in `Channel.java`.

- [ ] **Step 4: Create NotifyMailgunAutoConfiguration, add properties, add import**

Follow Discord auto-config pattern:
- `@ConditionalOnClass(name = "io.notifyhub.channel.mailgun.MailgunChannel")`
- `@ConditionalOnProperty(prefix = "notify.channels.mailgun", name = "api-key")`
- Add `Mailgun` inner class to `NotifyProperties.Channels` with fields: apiKey, domain, from, region
- Add `NotifyMailgunAutoConfiguration.class` to `@Import` list
- Add `notify-mailgun` as optional dependency to `notify-spring-boot-starter/pom.xml`

- [ ] **Step 5: Create SendMailgunTool and register in McpServerRunner**

Follow SendDiscordTool pattern:
- Tool name: `send_mailgun`
- Parameters: recipient, body, template, params, subject
- Uses `Channel.MAILGUN`
- Add `notify-mailgun` dependency to `notify-mcp/pom.xml`

- [ ] **Step 6: Run all tests**

Run: `mvn test -pl notify-channels/notify-mailgun -B`

- [ ] **Step 7: Commit**

```bash
git add notify-channels/notify-mailgun/ notify-core/ notify-spring-boot-starter/ notify-mcp/
git commit -m "feat: integrate Mailgun with tests, Spring Boot, and MCP"
```

---

## Chunk 7: New Channel — PagerDuty

### Task 25: PagerDuty Module + Config + Channel

**Files:**
- Create: `notify-channels/notify-pagerduty/pom.xml`
- Create: `notify-channels/notify-pagerduty/src/main/java/io/notifyhub/channel/pagerduty/PagerDutyConfig.java`
- Create: `notify-channels/notify-pagerduty/src/main/java/io/notifyhub/channel/pagerduty/PagerDutyChannel.java`
- Modify: `pom.xml` (root) — add module

- [ ] **Step 1: Add module to root pom.xml**

- [ ] **Step 2: Create pom.xml**

Same pattern — depends only on notify-core (uses JDK HttpClient, no external SDK).

- [ ] **Step 3: Create PagerDutyConfig**

```java
package io.notifyhub.channel.pagerduty;

import java.util.Set;

public class PagerDutyConfig {

    private static final Set<String> VALID_SEVERITIES = Set.of("critical", "error", "warning", "info");

    private final String routingKey;
    private final String severity;

    private PagerDutyConfig(Builder builder) {
        this.routingKey = requireNonBlank(builder.routingKey, "PagerDuty routing key");
        this.severity = builder.severity != null ? builder.severity : "warning";
        if (!VALID_SEVERITIES.contains(this.severity)) {
            throw new IllegalArgumentException(
                    "Severity must be one of: critical, error, warning, info. Got: " + this.severity);
        }
    }

    public String getRoutingKey() { return routingKey; }
    public String getSeverity() { return severity; }

    public static Builder builder() { return new Builder(); }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be null or blank");
        }
        return value;
    }

    public static class Builder {
        private String routingKey;
        private String severity;

        public Builder routingKey(String routingKey) { this.routingKey = routingKey; return this; }
        public Builder severity(String severity) { this.severity = severity; return this; }

        public PagerDutyConfig build() { return new PagerDutyConfig(this); }
    }
}
```

- [ ] **Step 4: Create PagerDutyChannel**

```java
package io.notifyhub.channel.pagerduty;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationChannel;
import io.notifyhub.core.channel.NotificationSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class PagerDutyChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(PagerDutyChannel.class);
    private static final String EVENTS_API_URL = "https://events.pagerduty.com/v2/enqueue";

    private final PagerDutyConfig config;
    private final HttpClient httpClient;

    public PagerDutyChannel(PagerDutyConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
    }

    PagerDutyChannel(PagerDutyConfig config, HttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    @Override
    public String getName() {
        return "pagerduty";
    }

    @Override
    public void send(Notification notification) {
        try {
            String content = notification.getRenderedContent();
            String summary = notification.getSubject() != null
                    ? notification.getSubject() + ": " + content
                    : content;

            // Truncate summary to PagerDuty's 1024 char limit
            if (summary.length() > 1024) {
                summary = summary.substring(0, 1021) + "...";
            }

            String json = """
                    {
                      "routing_key": "%s",
                      "event_action": "trigger",
                      "payload": {
                        "summary": "%s",
                        "severity": "%s",
                        "source": "notifyhub"
                      }
                    }
                    """.formatted(
                    escapeJson(config.getRoutingKey()),
                    escapeJson(summary),
                    config.getSeverity()
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EVENTS_API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 202) {
                throw new NotificationSendException("pagerduty",
                        "PagerDuty API returned " + response.statusCode() + ": " + response.body());
            }

            log.debug("PagerDuty event triggered successfully");

        } catch (NotificationSendException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationSendException("pagerduty",
                    "Failed to trigger PagerDuty event: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return config.getRoutingKey() != null && !config.getRoutingKey().isBlank();
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
```

- [ ] **Step 5: Compile**

Run: `mvn compile -pl notify-channels/notify-pagerduty -am -B -DskipTests`

- [ ] **Step 6: Commit**

```bash
git add pom.xml notify-channels/notify-pagerduty/
git commit -m "feat: add PagerDuty incident notification channel"
```

### Task 26: PagerDuty Tests + Spring + MCP + Rate Limits

**Files:**
- Create: `PagerDutyConfigTest.java`, `PagerDutyChannelTest.java`
- Modify: `Channel.java` — add PAGERDUTY
- Create: `NotifyPagerDutyAutoConfiguration.java`
- Modify: `NotifyProperties.java`, `NotifyAutoConfiguration.java`
- Create: `SendPagerDutyTool.java`
- Modify: `McpServerRunner.java`
- Modify: `RateLimitConfig.java` — add presets for new channels

- [ ] **Step 1: Create PagerDutyConfigTest**

```java
package io.notifyhub.channel.pagerduty;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PagerDutyConfigTest {

    @Test
    @DisplayName("Builder creates config with all fields")
    void builderCreatesConfig() {
        PagerDutyConfig config = PagerDutyConfig.builder()
                .routingKey("R0KEY000000000000000000000000000")
                .severity("critical")
                .build();
        assertEquals("R0KEY000000000000000000000000000", config.getRoutingKey());
        assertEquals("critical", config.getSeverity());
    }

    @Test
    @DisplayName("Default severity is warning")
    void defaultSeverity() {
        PagerDutyConfig config = PagerDutyConfig.builder()
                .routingKey("R0KEY").build();
        assertEquals("warning", config.getSeverity());
    }

    @Test
    @DisplayName("Builder throws on null routingKey")
    void requiresRoutingKey() {
        assertThrows(IllegalArgumentException.class, () ->
                PagerDutyConfig.builder().build());
    }

    @Test
    @DisplayName("Builder throws on invalid severity")
    void rejectsInvalidSeverity() {
        assertThrows(IllegalArgumentException.class, () ->
                PagerDutyConfig.builder().routingKey("key").severity("banana").build());
    }
}
```

- [ ] **Step 2: Create PagerDutyChannelTest**

```java
package io.notifyhub.channel.pagerduty;

import io.notifyhub.core.Notification;
import io.notifyhub.core.channel.NotificationSendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PagerDutyChannelTest {

    private PagerDutyConfig testConfig() {
        return PagerDutyConfig.builder()
                .routingKey("R0KEY000000000000000000000000000")
                .severity("warning")
                .build();
    }

    @Test
    @DisplayName("getName() returns 'pagerduty'")
    void getName() {
        assertEquals("pagerduty", new PagerDutyChannel(testConfig()).getName());
    }

    @Test
    @DisplayName("isAvailable() returns true when routing key is set")
    void isAvailable() {
        assertTrue(new PagerDutyChannel(testConfig()).isAvailable());
    }

    @Test
    @DisplayName("send() throws NotificationSendException on unreachable API")
    void sendFailsOnUnreachableApi() {
        PagerDutyChannel channel = new PagerDutyChannel(testConfig());
        Notification notification = new Notification(
                "ops-team", "pagerduty", "Alert", null, "Server down", Map.of());
        assertThrows(NotificationSendException.class, () -> channel.send(notification));
    }
}
```

- [ ] **Step 3: Add PAGERDUTY to Channel enum**

- [ ] **Step 4: Create NotifyPagerDutyAutoConfiguration, add properties, add import**

Follow Discord auto-config pattern:
- `@ConditionalOnClass(name = "io.notifyhub.channel.pagerduty.PagerDutyChannel")`
- `@ConditionalOnProperty(prefix = "notify.channels.pagerduty", name = "routing-key")`
- Add `PagerDuty` inner class to `NotifyProperties.Channels` with fields: routingKey, severity
- Add `NotifyPagerDutyAutoConfiguration.class` to `@Import` list
- Add `notify-pagerduty` as optional dependency to `notify-spring-boot-starter/pom.xml`

- [ ] **Step 5: Create SendPagerDutyTool and register in McpServerRunner**

Follow SendDiscordTool pattern:
- Tool name: `send_pagerduty`
- Parameters: recipient, body, template, params, subject, severity
- Uses `Channel.PAGERDUTY`
- Add `notify-pagerduty` dependency to `notify-mcp/pom.xml`

- [ ] **Step 7: Add rate limit presets for new channels**

In `RateLimitConfig.java`, add factory methods:
```java
/** PagerDuty Events API v2 — 120 events per minute. */
public static RateLimitConfig pagerduty() {
    return new RateLimitConfig(120, Duration.ofMinutes(1));
}

/** Mailgun API — 100 messages per minute. */
public static RateLimitConfig mailgun() {
    return new RateLimitConfig(100, Duration.ofMinutes(1));
}
```

Add to `forChannel()` switch:
```java
case "pagerduty" -> pagerduty();
case "mailgun" -> mailgun();
```

Add to `allDefaults()` map:
```java
defaults.put("pagerduty", pagerduty());
defaults.put("mailgun", mailgun());
```

- [ ] **Step 8: Run all tests**

Run: `mvn test -pl notify-channels/notify-pagerduty -B`

- [ ] **Step 9: Commit**

```bash
git add notify-channels/notify-pagerduty/ notify-core/ notify-spring-boot-starter/ notify-mcp/
git commit -m "feat: integrate PagerDuty with tests, Spring Boot, MCP, and rate limits"
```

---

## Chunk 8: Final Updates

### Task 27: Update CLAUDE.md + Docs

**Files:**
- Modify: `CLAUDE.md` — update module count, channel list, MCP tool count
- Modify: `docs/docs.html` — add AWS SNS, Mailgun, PagerDuty channel sections with Getting Credentials

- [ ] **Step 1: Update CLAUDE.md**

- Module count: 30 (was 27, +3 new channels)
- Channel enum: add AWS_SNS, MAILGUN, PAGERDUTY to the list
- MCP tools: 36
- Add new modules to module map
- Add `notify.channels.aws-sns.*`, `notify.channels.mailgun.*`, `notify.channels.pagerduty.*` to property prefix section

- [ ] **Step 2: Add new channels to docs.html**

Add 3 new channel sections following existing pattern with:
- YAML configuration example
- Java builder example
- Getting Credentials instructions

- [ ] **Step 3: Update CHANGELOG.md with final counts**

- [ ] **Step 4: Run full build**

Run: `mvn clean verify -B`
Expected: BUILD SUCCESS, all tests pass

- [ ] **Step 5: Commit**

```bash
git add CLAUDE.md CHANGELOG.md docs/docs.html
git commit -m "docs: update CLAUDE.md, CHANGELOG, and docs for 3 new channels"
```

### Task 28: Final Verification

- [ ] **Step 1: Run full test suite**

Run: `mvn clean verify -B`
Expected: All modules BUILD SUCCESS

- [ ] **Step 2: Verify test counts increased**

Check that notify-sms, notify-tracker-jpa, notify-audit-jpa, notify-aws-sns, notify-mailgun, notify-pagerduty all show tests running.

- [ ] **Step 3: Verify JaCoCo reports generated**

Check: `ls notify-core/target/site/jacoco/jacoco.xml`

- [ ] **Step 4: Push to remote**

Push to current branch. If on a worktree branch, merge to master first (ask user for confirmation before pushing).

# NotifyHub Completeness & Production-Readiness Design

**Date:** 2026-03-16
**Status:** Approved
**Scope:** Tests, CI/CD, community files, documentation, new channels

---

## 1. Overview

Close all gaps to make NotifyHub production-ready, community-friendly, and complete for Maven Central 1.0.0 release. Five workstreams executed in order (bottom-up approach).

## 2. Workstream 1: Missing Tests + Code Coverage

### 2.1 Tests to Create

**notify-channels/notify-sms** (0 tests today):
- `TwilioSmsChannelTest.java` — getName, isAvailable, config validation, send fails without credentials
- `TwilioConfigTest.java` — builder, defaults, validation (accountSid, authToken, fromNumber required)
- Note: `TwilioWhatsAppChannel` in this module is legacy — replaced by `notify-whatsapp` (WhatsApp Cloud API). No new tests needed for it.

**notify-tracker-jpa** (0 tests today):
- `JpaDeliveryTrackerTest.java` — save receipt, find by id, find by status, find by channel

**notify-audit-jpa** (0 tests today):
- `JpaAuditLoggerTest.java` — log success, log failure, query by date range, query by channel

### 2.2 Test Patterns

All tests follow existing conventions:
- `@ExtendWith(MockitoExtension.class)`
- `@DisplayName` on every test method
- Mock external dependencies (Twilio SDK, JPA EntityManager)
- Test config validation in builders (null, blank, valid)

### 2.3 JaCoCo Integration

- Add `jacoco-maven-plugin` to root `pom.xml` `<pluginManagement>`
- Generate report during `verify` phase
- Aggregate report across modules
- **No global threshold** — only generate reports for now. Enforcing a minimum would break modules with zero tests (admin, tracker-jpa, audit-jpa). Threshold can be added per-module later.
- Upload to Codecov in CI workflow (Codecov handles per-module reporting)

## 3. Workstream 2: Community Files

### 3.1 Files to Create

| File | Purpose |
|------|---------|
| `CONTRIBUTING.md` | Setup local, code conventions, how to create a channel, PR guidelines |
| `CHANGELOG.md` | Keep a Changelog format, document 1.0.0 features |
| `CODE_OF_CONDUCT.md` | Contributor Covenant v2.1 |
| `SECURITY.md` | Vulnerability reporting process (email, not public issue) |
| `.github/ISSUE_TEMPLATE/bug_report.md` | Bug report template with repro steps |
| `.github/ISSUE_TEMPLATE/feature_request.md` | Feature request template |
| `.github/ISSUE_TEMPLATE/new_channel.md` | New channel suggestion template |
| `.github/pull_request_template.md` | PR checklist (tests, docs, changelog) |

### 3.2 CONTRIBUTING.md Structure

1. Prerequisites (Java 17+, Maven 3.8+)
2. Fork & clone
3. Build & test (`mvn clean verify -B`)
4. Code conventions (from CLAUDE.md, summarized)
5. Creating a new channel (step-by-step recipe)
6. Submitting a PR (branch naming, commit messages, checklist)

### 3.3 CHANGELOG.md

Follow [Keep a Changelog](https://keepachangelog.com/) format:
```
## [1.0.0] - 2026-03-16
### Added
- 23 notification channels (Email, SMS, WhatsApp, Slack, ..., AWS SNS, Mailgun, PagerDuty)
- MCP Server with 36 tools for AI agent integration
- Spring Boot 3.x auto-configuration with per-channel activation
- ...
```
Note: Final tool/channel counts depend on workstream 5 completion. Update CHANGELOG at the end.

## 4. Workstream 3: CI/CD + Security

### 4.1 Dependabot

`.github/dependabot.yml`:
- Maven ecosystem: weekly schedule, max 10 open PRs
- GitHub Actions ecosystem: weekly schedule

### 4.2 CodeQL Analysis

`.github/workflows/codeql.yml`:
- Trigger: push to master, PRs
- Language: Java
- Uses `github/codeql-action` v3

### 4.3 CI Enhancements

Update `.github/workflows/ci.yml`:
- Add JaCoCo report upload to Codecov
- Add coverage badge to README

### 4.4 README Badges

Add after existing badges:
- Codecov coverage badge
- CodeQL security badge

## 5. Workstream 4: Documentation + DX

### 5.1 Architecture Diagram

Add to `docs/docs.html` in a new "Architecture" section:
- Hand-crafted inline SVG diagram showing: App → NotifyHub → Channel Selection → Provider
- Show retry loop, rate limiter, dead letter queue in the flow
- Show fallback mechanism
- Styled with existing CSS variables (--accent, --bg2, --text, etc.)

### 5.2 Quick Start Guides

Add to `docs/docs.html` after the Getting Started section:
1. **"Send your first email"** — minimal 5-line Java example
2. **"Multi-channel with fallback"** — email primary, Slack fallback
3. **"Scheduled notifications"** — schedule + cancel example
4. **"Audience broadcasting"** — contacts, tags, audience, broadcast

### 5.3 Custom Channel Guide

Add to `docs/docs.html`:
- Complete PagerDuty channel example (Config + Channel + test)
- Step-by-step explanation matching CLAUDE.md recipe

### 5.4 i18n Example

The `NotificationBuilder.locale(Locale)` method already exists in notify-core. Document in docs how to use it:
```java
hub.notify()
    .to("user@email.com")
    .via(Channel.EMAIL)
    .template("welcome", Map.of("name", "Gabriel"))
    .locale(Locale.forLanguageTag("pt-BR"))
    .send();
```
With template file structure: `templates/notify/welcome/pt-BR.mustache`
No core code changes needed — this is documentation only.

### 5.5 Version Sync

- Update CLAUDE.md version from `0.9.0` to `1.0.0` to match pom.xml (pom.xml is already at 1.0.0, only CLAUDE.md needs updating)

## 6. Workstream 5: New Channels

### 6.1 AWS SNS (`notify-aws-sns`)

- **Config:** `AwsSnsConfig` — region, accessKeyId, secretAccessKey, topicArn (optional)
- **Channel:** `AwsSnsChannel` — uses AWS SDK v2 `SnsClient`
- **getName():** `"aws-sns"`
- **send():** Publish to topic (if topicArn in config) or direct SMS/push (if recipient is phone/endpoint ARN)
- **Dependency:** `software.amazon.awssdk:sns` (managed via `software.amazon.awssdk:bom:2.25.0` in root pom `<dependencyManagement>`)
- **Spring:** `NotifyAwsSnsAutoConfiguration`, properties under `notify.channels.aws-sns.*`
- **MCP:** `SendAwsSnsTool`
- **Tests:** `AwsSnsChannelTest`, `AwsSnsConfigTest`

### 6.2 Mailgun (`notify-mailgun`)

- **Config:** `MailgunConfig` — apiKey, domain, from (required); region (optional, default US)
- **Channel:** `MailgunChannel` — uses JDK HttpClient to POST to Mailgun API
- **getName():** `"mailgun"`
- **send():** POST to `https://api.mailgun.net/v3/{domain}/messages`
- **Auth:** Basic auth with `api:{apiKey}`
- **Spring:** `NotifyMailgunAutoConfiguration`, properties under `notify.channels.mailgun.*`
- **MCP:** `SendMailgunTool`
- **Tests:** `MailgunChannelTest`, `MailgunConfigTest`

### 6.3 PagerDuty (`notify-pagerduty`)

- **Config:** `PagerDutyConfig` — routingKey (required), severity (optional, default "warning")
- **Channel:** `PagerDutyChannel` — uses Events API v2
- **getName():** `"pagerduty"`
- **send():** POST to `https://events.pagerduty.com/v2/enqueue`
- **Payload:** Event with routing_key, event_action "trigger", summary from notification
- **Spring:** `NotifyPagerDutyAutoConfiguration`, properties under `notify.channels.pagerduty.*`
- **MCP:** `SendPagerDutyTool`
- **Tests:** `PagerDutyChannelTest`, `PagerDutyConfigTest`

### 6.4 Core Changes

- Add to `Channel` enum: `AWS_SNS`, `MAILGUN`, `PAGERDUTY`
- Note: `WEBHOOK` and `SENDGRID` already work without dedicated enum entries — they use the generic `NotificationChannel` interface. The enum is a convenience shortcut, not a requirement. New channels get enum entries for consistency.
- Add rate limit presets in `RateLimitConfig` for new channels
- Add AWS SDK BOM to root `pom.xml` `<dependencyManagement>`: `software.amazon.awssdk:bom:2.25.0`
- Add 3 new modules to root `pom.xml` `<modules>`
- Update CLAUDE.md module map: 23 channel modules total (20 existing + 3 new), 36 MCP tools (33 existing + 3 new Send*Tools)

**Final channel count:** Email, SMS, WhatsApp (Cloud + Twilio legacy), Slack, Telegram, Discord, Teams, Push (Firebase), Webhook, WebSocket, Google Chat, Twitter, LinkedIn, Notion, Twitch, YouTube, Instagram, SendGrid, TikTok Shop, Facebook, AWS SNS, Mailgun, PagerDuty = **23 modules**

## 7. Execution Order

1. Tests (notify-sms, tracker-jpa, audit-jpa) + JaCoCo
2. Community files (CONTRIBUTING, CHANGELOG, templates, CoC, SECURITY)
3. CI/CD (Dependabot, CodeQL, coverage upload, badges)
4. Docs (architecture diagram, quick starts, custom channel guide, i18n, version sync)
5. New channels (AWS SNS, Mailgun, PagerDuty) — each with full stack (config, channel, spring, MCP, tests, docs)

## 8. Out of Scope

- Credential encryption at rest (user responsibility via Spring Cloud Vault, AWS Secrets Manager)
- Circuit breaker pattern (existing fallback mechanism is sufficient)
- Swagger/OpenAPI (belongs to demo app, not the library)
- Separate Javadoc site (Maven Central publishes javadoc JARs automatically)
- Additional messengers (Signal, Viber, Line) — low demand, limited APIs
- AWS SES — redundant with SendGrid + Mailgun

## 9. Success Criteria

- All channel modules and core modules have at least 1 test file
- JaCoCo reports generated (no enforced threshold initially)
- All community files present and linked from README
- CodeQL and Dependabot active
- 23 channels documented with getting credentials
- 3 new channels fully integrated (config, channel, spring, MCP, tests, docs)
- CLAUDE.md and pom.xml versions aligned at 1.0.0

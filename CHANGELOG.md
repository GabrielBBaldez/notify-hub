# Changelog

All notable changes to NotifyHub will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/), and this project adheres to [Semantic Versioning](https://semver.org/).

## [0.9.0] - 2026-03-16

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
- JaCoCo code coverage reporting
- Dependabot for Maven and GitHub Actions dependency updates
- CodeQL security analysis workflow
- Codecov coverage upload and badge
- CONTRIBUTING.md, CODE_OF_CONDUCT.md, SECURITY.md
- GitHub issue templates (bug report, feature request, new channel) and PR template
- Rate limit presets for PagerDuty (120/min) and Mailgun (100/min)

### Security
- HMAC-SHA256 webhook signature verification
- OAuth2 JWT assertion flow for Firebase Cloud Messaging
- Facebook access token moved from URL to Authorization header

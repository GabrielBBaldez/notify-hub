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

- **Bugs**: Use the Bug Report template
- **Features**: Use the Feature Request template
- **New Channels**: Use the New Channel template
- **Security**: See [SECURITY.md](SECURITY.md) for vulnerability reporting

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

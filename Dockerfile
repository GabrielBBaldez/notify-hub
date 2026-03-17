# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY notify-core/pom.xml notify-core/
COPY notify-channels/ notify-channels/
COPY notify-tracker-jpa/pom.xml notify-tracker-jpa/
COPY notify-audit-jpa/pom.xml notify-audit-jpa/
COPY notify-queue-rabbitmq/pom.xml notify-queue-rabbitmq/
COPY notify-queue-kafka/pom.xml notify-queue-kafka/
COPY notify-spring-boot-starter/pom.xml notify-spring-boot-starter/
COPY notify-admin/pom.xml notify-admin/
COPY notify-demo/pom.xml notify-demo/
COPY notify-mcp/pom.xml notify-mcp/
RUN mvn dependency:go-offline -pl notify-mcp -am -B 2>/dev/null || true
COPY . .
RUN mvn clean package -pl notify-mcp -am -DskipTests -B -q

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="NotifyHub MCP Server" \
      org.opencontainers.image.description="Unified notification MCP server — 36 tools, 23 channels, one API" \
      org.opencontainers.image.url="https://github.com/GabrielBBaldez/notify-hub" \
      org.opencontainers.image.source="https://github.com/GabrielBBaldez/notify-hub" \
      org.opencontainers.image.licenses="MIT" \
      io.modelcontextprotocol.server.name="io.github.gabrielbbaldez/notify-hub"

RUN addgroup -S notifyhub && adduser -S notifyhub -G notifyhub
RUN mkdir -p /home/notifyhub/.notifyhub && chown notifyhub:notifyhub /home/notifyhub/.notifyhub
USER notifyhub
WORKDIR /app
COPY --from=build /app/notify-mcp/target/notify-mcp-*.jar notify-mcp.jar
ENTRYPOINT ["java", "-jar", "notify-mcp.jar"]

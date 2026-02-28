FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY .. /app
RUN cd /app && ./mvnw -pl notify-checkout -am package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/notify-checkout/target/*.jar app.jar
RUN mkdir -p /app/data
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]

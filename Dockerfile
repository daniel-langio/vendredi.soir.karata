# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY src ./src
COPY doc ./doc

# Tests need Testcontainers (Docker-in-Docker) and already run in CI - skip them here so the
# image build only needs to compile and package.
RUN ./gradlew bootJar -x test --no-daemon

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

# Env vars (PORT, DATABASE_URL, JWT_SECRET, ...) are picked up directly by Spring from
# application.properties' placeholders - no shell wrapper needed. DATABASE_URL specifically gets
# reshaped from Render's postgres:// form into what Spring's JDBC driver expects by
# RenderDatabaseUrlEnvironmentPostProcessor, inside the app itself.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

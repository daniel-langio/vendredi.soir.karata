# syntax=docker/dockerfile:1

# ---- Web UI build stage ----
# Matches the old same-origin setup (see cd-compute.yml / .shell/build_web_ui.sh): the SPA is
# built here and copied into src/main/resources/static/ before the Java build below, so Spring
# serves it from the same origin as /poker/* - no CORS needed.
FROM ghcr.io/cirruslabs/flutter:3.41.2 AS web-ui-build
WORKDIR /repo

COPY .shell/build_web_ui.sh .shell/build_web_ui.sh
COPY web-ui web-ui
RUN chmod +x .shell/build_web_ui.sh && .shell/build_web_ui.sh

# ---- Build stage (Java) ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY src ./src
COPY doc ./doc
COPY --from=web-ui-build /repo/src/main/resources/static ./src/main/resources/static

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

# ── Stage 1: Build ───────────────────────────────────────────────────────────
# Uses the official Maven image with Amazon Corretto 21 to compile and package the app.
# This stage is discarded after the build — only the JAR is kept.
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml first so Docker cache reuses the dependency layer
# when only source code changes (not dependencies).
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build the fat JAR
COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
# Minimal JRE-only image — much smaller than the full JDK build image.
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Create a non-root user — security best practice
RUN groupadd --system appgroup && \
    useradd --system --gid appgroup --no-create-home appuser

# Copy only the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Set ownership
RUN chown appuser:appgroup app.jar

USER appuser

# Document the port (informational — does not publish it)
EXPOSE 8080

# Health check used by Docker and ECS
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]

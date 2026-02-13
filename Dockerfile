# Stage 1: Build stage

ENTRYPOINT ["java", "-jar", "app.jar"]
# Run the application

  CMD curl -f http://localhost:8080/health || exit 1
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
# Health check

ENV DB_PASSWORD=postgres
ENV DB_USER=postgres
ENV DB_NAME=todo_db
ENV DB_PORT=5432
ENV DB_HOST=postgres
# Set environment variables for database connection

EXPOSE 8080
# Update this port based on your actual application server port
# Expose the port your application will run on

COPY --from=builder /app/build/libs/*.jar app.jar
# Copy the built JAR from the builder stage

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
# Install curl for health checks

WORKDIR /app

FROM openjdk:17-jdk-slim
# Stage 2: Runtime stage

RUN gradle build -x test --no-daemon
# Build the application

COPY src ./src
# Copy source code

COPY gradle ./gradle
COPY build.gradle settings.gradle gradlew ./
# Copy gradle files

WORKDIR /app

FROM gradle:8-jdk17 as builder

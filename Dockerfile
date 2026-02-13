# Stage 1: Build stage
FROM gradle:8-jdk17 as builder

WORKDIR /app

# Copy gradle files
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Copy source code
COPY src ./src

# Build the application
RUN gradle build -x test --no-daemon

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy the built JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose the port your application will run on
EXPOSE 8080

# Set environment variables for database connection
ENV DB_HOST=postgres
ENV DB_PORT=5455
ENV DB_NAME=todo_db
ENV DB_USER=postgres
ENV DB_PASSWORD=postgres

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

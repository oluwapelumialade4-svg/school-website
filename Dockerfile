# Multi-Stage Dockerfile for Render Deployment
# Phase 1: Build Stage
FROM maven:3.9.4-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml first for better layer caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Phase 2: Runtime Stage
FROM eclipse-temurin:17-jre-alpine

# Create app directory
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Expose port 8081 (matches server.port)
EXPOSE 8081

# Set the entrypoint to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
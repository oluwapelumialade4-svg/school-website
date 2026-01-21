# Use Java 21 instead of 17
FROM eclipse-temurin:21-jdk-alpine

# Set the working directory
WORKDIR /app

# Copy the build file from your computer to the server
COPY target/*.jar app.jar

# Tell Render which port to use (8081 as you've been using)
EXPOSE 8081

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
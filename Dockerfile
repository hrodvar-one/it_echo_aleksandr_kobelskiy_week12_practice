# Use OpenJDK 21 as the base image
FROM openjdk:21-jdk-slim

# Set the working directory
WORKDIR /app

# Install netcat (using netcat-openbsd)
RUN apt-get update && apt-get install -y netcat-openbsd && rm -rf /var/lib/apt/lists/*

# Copy Gradle wrapper and build configuration files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy gradle.properties
COPY gradle.properties .

# Copy source code
COPY src src

# Copy wait-for-it script
COPY wait-for-it.sh /wait-for-it.sh
RUN chmod +x /wait-for-it.sh

# Ensure Gradle Wrapper has the right permissions
RUN chmod +x gradlew

# Pre-download dependencies
RUN ./gradlew dependencies --no-daemon

# Build the project, skipping tests
RUN ./gradlew clean build -x test --no-daemon

# Expose the application port
EXPOSE 8080

# Set the default command to run the application using wait-for-it
CMD ["/wait-for-it.sh", "db", "--", "java", "-jar", "build/libs/week12practice-1.0.0.jar"]

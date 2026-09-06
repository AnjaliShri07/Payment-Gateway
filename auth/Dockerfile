# Stage 1: Build the application
FROM maven:3.9.6-openjdk-22 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests -X

# Stage 2: Run the application
FROM openjdk:22-jdk-slim
WORKDIR /app
COPY target/kafka-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8181
ENTRYPOINT ["java", "-jar", "app.jar"]
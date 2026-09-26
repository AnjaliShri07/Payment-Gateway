# Build the Spring Boot application with the same Java release configured in pom.xml.
FROM maven:3.9.11-eclipse-temurin-26 AS build

WORKDIR /workspace

COPY pom.xml .
COPY src src

#RUN mvn -B -DskipTests clean package

# Keep the runtime image smaller than the build image.
FROM eclipse-temurin:26-jre

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8081

USER 1001

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

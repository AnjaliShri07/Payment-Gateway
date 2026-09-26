# Build stage
FROM maven:3.9.11-eclipse-temurin-26 AS build

WORKDIR /build

# Copy the build descriptor first so Jenkins/Docker can reuse the dependency layer.
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:26-jre

WORKDIR /app

RUN groupadd --system --gid 10001 appgroup \
    && useradd --system --uid 10001 --gid 10001 --no-create-home appuser

COPY --from=build --chown=appuser:appgroup /build/target/auth-0.0.1-SNAPSHOT.jar /app/app.jar

USER 10001:10001

EXPOSE 8082

# Spring Boot honors JAVA_TOOL_OPTIONS, allowing Kubernetes resource-aware JVM tuning.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=25.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

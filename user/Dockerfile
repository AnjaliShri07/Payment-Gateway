# Build stage
FROM eclipse-temurin:26-jdk AS build

WORKDIR /build

# Copy the build descriptor first so Docker can reuse the dependency layer.
COPY mvnw ./
COPY .mvn ./.mvn
COPY pom.xml .
RUN chmod +x ./mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src ./src
RUN ./mvnw -B -ntp clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:26-jre

WORKDIR /app

RUN groupadd --system --gid 10001 appgroup \
    && useradd --system --uid 10001 --gid 10001 --no-create-home appuser

COPY --from=build --chown=appuser:appgroup /build/target/user-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8081

USER 10001:10001

ENV SERVER_PORT=8081
ENV JAVA_TOOL_OPTIONS="-Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

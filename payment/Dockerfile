FROM eclipse-temurin:26-jdk AS build

WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -B -Djava.version=26 -Dmaven.test.skip=true package

FROM eclipse-temurin:26-jre

WORKDIR /app
COPY --from=build /workspace/target/payment-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8083
USER 10001
ENTRYPOINT ["java", "-jar", "app.jar"]

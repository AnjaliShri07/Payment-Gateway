# Payment Gateway Microservice

A **Spring Boot** microservice for payment processing, event streaming, and audit logging, backed by MySQL, MongoDB, and Apache Kafka.

## Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 26 |
| **Framework** | Spring Boot 4.1 |
| **Relational DB** | MySQL — transaction storage |
| **Document DB** | MongoDB — audit log store |
| **Messaging** | Apache Kafka — asynchronous event streaming |
| **API Docs** | Swagger / OpenAPI 3 |
| **Container** | Docker (optional) |

## Prerequisites

- Java 26+
- Maven 3.9+
- MySQL running on `localhost:3306`
- MongoDB running on `localhost:27017`
- Zookeeper and Kafka installed and running; Kafka must be reachable at `localhost:9092`

This project is configured to use your local Kafka broker (`localhost:9092`). Start Zookeeper and Kafka using the scripts/configuration from your Kafka installation before starting the application. You do not need to start Docker Kafka or Zookeeper.

The default database settings are `payment_gateway` in MySQL and `payment_audit` in MongoDB. See `src/main/resources/application.yml` or the environment-variable table below to override the connection settings.

## Run Locally

From the `payment` directory, build and start the application:

```bash
mvn clean package -DskipTests
mvn spring-boot:run
```

Or run the packaged JAR:

```bash
java -jar target/payment-0.0.1-SNAPSHOT.jar
```

The service listens on port `8083` by default.

| Endpoint | URL |
|---|---|
| **Swagger UI** | http://localhost:8083/swagger-ui.html |
| **Health Check** | http://localhost:8083/actuator/health |
| **API Docs (JSON)** | http://localhost:8083/v3/api-docs/payment |
| **Payments API** | http://localhost:8083/api/v1/payments |

### Payment API Quick Reference

All payment endpoints require an authenticated caller. List the caller's payments with:

```http
GET /api/v1/payments
```

Filter that list by a payment status using the `status` query parameter:

```http
GET /api/v1/payments?status=COMPLETED
```

Valid status values are `INITIATED`, `PROCESSING`, `AUTHORIZED`, `COMPLETED`, `FAILED`, `REFUNDED`, and `CANCELLED`. An unknown value returns `400 Bad Request`.

MongoDB audit-log responses and Kafka payment events group user information under `payload` and payment information under `payment`. Sensitive card verification data is not stored in MongoDB.

Errors use a consistent JSON response with `timestamp`, `status`, `error`, `message`, and `path`; validation errors include `validationErrors`. Attempts to capture or refund a failed payment return `422 Unprocessable Entity` with the recorded failure message.

## Optional: Build the Application Docker Image

The repository includes a `Dockerfile` for building the payment-service image:

```bash
docker build -t payment-service .
```

The application container must be configured to connect to reachable MySQL, MongoDB, and Kafka hosts. In particular, `localhost` inside a container refers to that container, not the host machine. Set the connection environment variables for your Docker network before running the image. This repository does not include a Docker Compose file.

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8083` | Payment service HTTP port |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/payment_gateway` | MySQL connection URL |
| `SPRING_DATASOURCE_USERNAME` | `root` | MySQL username |
| `SPRING_DATASOURCE_PASSWORD` | `root` | MySQL password |
| `APP_MONGODB_URI` | `mongodb://localhost:27017` | MongoDB server connection URI |
| `APP_MONGODB_DATABASE` | `payment_audit` | MongoDB database name |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker address |
| `APP_WEBHOOK_SECRET` | *(application default)* | HMAC secret for webhook validation |

## Related Documentation

- [Payment Architecture & Postman Guide](./PAYMENT_ARCHITECTURE_AND_POSTMAN_GUIDE.md) — API reference, Kafka topics, and Postman examples

# User Management Service

## 1. Purpose

The User Management Service owns user profile data for the Payment Gateway. It exposes secured REST APIs for user administration, persists users and roles in MySQL, and delegates bearer-token validation to the Auth microservice.

```text
Client
  |
  | JWT bearer token
  v
User Service (8081)
  |-- TokenAuthenticationFilter
  |-- UserController
  |-- UserServiceImpl
  |-- UserRepository
  |-- MySQL
  |
  +--> Auth Service (8082)
        token validation and authenticated profile
```

## 2. Proposed project structure

```text
user/
|-- pom.xml
|-- README.md
|-- src/
|   |-- main/
|   |   |-- java/org/paymentgateway/user/
|   |   |   |-- UserApplication.java
|   |   |   |
|   |   |   |-- controller/
|   |   |   |   `-- UserController.java
|   |   |   |
|   |   |   |-- service/
|   |   |   |   |-- BaseService.java
|   |   |   |   |-- AbstractBaseService.java
|   |   |   |   `-- UserServiceImpl.java
|   |   |   |
|   |   |   |-- repository/
|   |   |   |   |-- BaseRepository.java
|   |   |   |   `-- UserRepository.java
|   |   |   |
|   |   |   |-- entity/
|   |   |   |   |-- BaseEntity.java
|   |   |   |   |-- User.java
|   |   |   |   `-- Role.java
|   |   |   |
|   |   |   |-- DTO/
|   |   |   |   |-- BaseResponse.java
|   |   |   |   `-- UserUpdateRequest.java
|   |   |   |
|   |   |   |-- client/
|   |   |   |   |-- AuthenticationServiceClient.java
|   |   |   |   `-- dto/
|   |   |   |       |-- AuthApiResponse.java
|   |   |   |       `-- AuthUserProfile.java
|   |   |   |
|   |   |   |-- config/
|   |   |   |   |-- OpenApiConfig.java
|   |   |   |   `-- SecurityConfig.java
|   |   |   |
|   |   |   |-- exception/
|   |   |   |   |-- GlobalExceptionHandler.java
|   |   |   |   `-- ResourceNotFoundException.java
|   |   |   |
|   |   |   `-- constant/
|   |   |       `-- UserRole.java
|   |   |
|   |   `-- resources/
|   |       `-- application.yml
|   |
|   `-- test/
|       `-- java/org/paymentgateway/user/
|           |-- UserApplicationTests.java
|           |-- client/AuthenticationServiceClientTest.java
|           |-- config/SecurityConfigTest.java
|           |-- controller/UserControllerTest.java
|           |-- exception/GlobalExceptionHandlerTest.java
|           `-- service/UserServiceImplTest.java
```

## 3. Component responsibilities

| Component | Responsibility |
|---|---|
| `UserApplication` | Starts the Spring Boot application. |
| `UserController` | Exposes HTTP endpoints, validates request bodies, maps service results to `ResponseEntity<BaseResponse<T>>`, and provides circuit-breaker fallbacks. |
| `UserServiceImpl` | Implements user CRUD and partial-update behavior. It updates only fields supplied in `UserUpdateRequest`. |
| `BaseService` / `AbstractBaseService` | Provides reusable service contracts and common CRUD operations. |
| `UserRepository` | Provides Spring Data JPA persistence operations for `User`. |
| `User` | JPA aggregate containing profile, account-status, and role-assignment data. |
| `Role` | JPA entity representing an assignable user role. |
| `UserUpdateRequest` | Nullable command object for partial user updates; it is intentionally separate from the persistence entity. |
| `AuthenticationServiceClient` | Calls the Auth microservice to validate bearer tokens and retrieve the authenticated profile. |
| `TokenAuthenticationFilter` | Intercepts bearer tokens, obtains the external profile, and populates Spring Security's `SecurityContext`. |
| `GlobalExceptionHandler` | Converts validation, not-found, and unexpected exceptions into the common response format. |
| `OpenApiConfig` | Registers `RestTemplate` and OpenAPI/Swagger metadata. |

## 4. REST API

Base path: `/api/v1/users`

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/users` | Fetch all users. |
| `GET` | `/api/v1/users/{id}` | Fetch one user by ID. |
| `PUT` | `/api/v1/users/{id}` | Partially update a user. |
| `DELETE` | `/api/v1/users/{id}` | Delete a user. |

All user endpoints require an `Authorization: Bearer <JWT>` header. Responses use:

```json
{
  "status": "SUCCESS",
  "message": "Record found",
  "data": {}
}
```

### Partial update example

```http
PUT /api/v1/users/42
Authorization: Bearer <JWT>
Content-Type: application/json
```

```json
{
  "email": "new-address@example.com"
}
```

Only `email` is changed. Omitted fields remain unchanged.

## 5. Request flow

1. `TokenAuthenticationFilter` reads the bearer token.
2. `AuthenticationServiceClient` calls `GET http://localhost:8082/api/v1/users/me`.
3. A valid profile is converted into a Spring Security authentication.
4. `UserController` validates the request and delegates to `UserServiceImpl`.
5. `UserServiceImpl` reads or changes the aggregate through `UserRepository`.
6. `GlobalExceptionHandler` standardizes validation and error responses.

## 6. Persistence model

The service uses MySQL database `payment_gateway`.

- `users` stores user credentials, profile information, and account-status flags.
- `roles` stores role definitions.
- `user_roles` stores the many-to-many user-role association.
- `BaseEntity` provides `createdAt` and `updatedAt` auditing fields.
- Hibernate schema management is configured as `validate`; schema changes should be managed through an explicit database migration strategy.

## 7. Security and resilience

- Stateless Spring Security configuration.
- JWT validation delegated to the Auth microservice.
- Swagger and health endpoints are publicly accessible.
- Other application endpoints require authentication.
- Resilience4j protects selected read endpoints with the `userServiceCB` circuit breaker.
- Authentication-client failures produce an empty authentication result rather than trusting an unvalidated token.

## 8. Configuration

Important settings are in `src/main/resources/application.yml`:

| Setting | Current value |
|---|---|
| HTTP port | `8081` |
| Auth service URL | `http://localhost:8082` |
| Database | `jdbc:mysql://localhost:3306/payment_gateway` |
| OpenAPI JSON | `/v3/api-docs/user` |
| Swagger UI | `/swagger-ui.html` |
| Schema mode | `validate` |

Database credentials and service URLs should be supplied through environment-specific configuration or environment variables rather than committed defaults.

## 9. Testing strategy

The module uses JUnit 5 and Mockito for isolated tests:

- Service tests mock `UserRepository`.
- Controller tests mock `UserServiceImpl` and `AuthenticationServiceClient`.
- Client tests mock `RestTemplate`.
- Security tests use mock servlet requests, responses, and filter chains.
- Exception-handler tests verify HTTP status and response payloads.
- `UserApplicationTests` remains the Spring context smoke test.

Run the module tests with:

```powershell
./mvnw.cmd test
```

## 10. Recommended next improvements

1. Introduce database migrations, such as Flyway or Liquibase, instead of relying only on Hibernate validation.
2. Move secrets and environment-specific URLs out of `application.yml`.
3. Add explicit response DTOs so JPA entities are not serialized directly from controllers.
4. Return `Optional` from service lookup methods where absence is expected and handle it at the controller boundary.
5. Add endpoint-level `MockMvc` tests in addition to direct Mockito controller tests.
6. Add authorization rules for role-based operations such as user administration and role changes.
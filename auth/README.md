# Industry-Standard Spring Boot JWT Authentication & Security System

This plan details the end-to-end architecture and implementation of an enterprise-grade, industry-standard JWT authentication and authorization system using Spring Boot (modern 3.x/4.x compatible structure), Java 26 / modern Java features (Records, Pattern Matching), Spring Security 6+, JJWT, and JPA.

## Architecture Highlights

1. **Stateless Security Architecture**: Spring Security `SecurityFilterChain` with `SessionCreationPolicy.STATELESS`, CSRF disabled for REST, strictly configured CORS, and Jwt exception handling.
2. **Access Token + Refresh Token Flow with Rotation**: Short-lived Access Tokens (e.g., 15 minutes) paired with long-lived, database-backed Refresh Tokens (e.g., 7 days) featuring **Token Rotation** to mitigate replay attacks.
3. **Role-Based Access Control (RBAC)**: Fine-grained roles (`ROLE_USER`, `ROLE_MODERATOR`, `ROLE_ADMIN`) with `@EnableMethodSecurity` and URL pattern matching.
4. **Modern Java & Clean Layering**: Immutability via Java Records for DTOs, Domain Entities with JPA Auditing, Service Layer decoupling, and Centralized Global Exception Handling (`@RestControllerAdvice`).
5. **Modern JJWT Integration (0.12.x+)**: Zero deprecated methods, strong cryptographic signing using HMAC-SHA256 / SHA512 with high-entropy keys.
6. **OpenAPI 3 / Swagger Documentation**: Pre-configured interactive API documentation to easily test authentication headers, login, and protected routes.

---

The service currently uses MySQL (`payment_gateway`) and Hibernate schema mode `update`; see `src/main/resources/application.yml` for connection settings. Do not rely on the development database credentials or automatic schema updates for production.

---

## Proposed Project Structure & Components

```
auth/
├── src/
│   ├── main/
│   │   ├── java/org/paymentgateway/auth/
│   │   │   ├── AuthApplication.java
│   │   │   │
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── JpaAuditingConfig.java
│   │   │   │   └── OpenApiConfig.java
│   │   │   │
│   │   │   ├── controller/
│   │   │   │   ├── AuthenticationController.java
│   │   │   │   ├── UserController.java
│   │   │   │   └── AdminController.java
│   │   │   │
│   │   │   ├── service/
│   │   │   │   ├── AuthenticationService.java
│   │   │   │   ├── RefreshTokenService.java
│   │   │   │   ├── UserService.java
│   │   │   │   └── ClientApplicationService.java
│   │   │   │
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── RoleRepository.java
│   │   │   │   ├── RefreshTokenRepository.java
│   │   │   │   └── ClientApplicationRepository.java
│   │   │   │
│   │   │   ├── entity/
│   │   │   │   ├── BaseEntity.java
│   │   │   │   ├── JwtUser.java
│   │   │   │   ├── Role.java
│   │   │   │   ├── RefreshToken.java
│   │   │   │   └── ClientApplication.java
│   │   │   │
│   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   │
│   │   │   ├── security/
│   │   │   │   ├── JwtUserDetails.java
│   │   │   │   ├── JwtUserDetailsService.java
│   │   │   │   ├── JwtAccessDeniedHandler.java
│   │   │   │   └── jwt/
│   │   │   │       ├── JwtTokenProvider.java
│   │   │   │       ├── JwtAuthenticationFilter.java
│   │   │   │       └── JwtAuthenticationEntryPoint.java
│   │   │   │
│   │   │   ├── validation/
│   │   │   │   ├── annotation/
│   │   │   │   ├── validator/
│   │   │   │   └── service/
│   │   │   │
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── BadRequestException.java
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   ├── UserNotFoundException.java
│   │   │   │   ├── UserAlreadyExistsException.java
│   │   │   │   ├── TokenExpiredException.java
│   │   │   │   └── TokenRefreshException.java
│   │   │   │
│   │   │   └── constants/
│   │   │       ├── ERole.java
│   │   │       └── SecurityConstants.java
│   │   │
│   │   └── resources/
│   │       └── application.yml
│   │
│   └── test/
│       └── java/org/paymentgateway/auth/
│           ├── AuthApplicationTests.java
│           ├── ControllerTests.java
│           ├── JwtSecurityTests.java
│           ├── RefreshTokenServiceTests.java
│           └── UserAndClientServiceTests.java
```

---

## Proposed Changes

### 1. Build & Core Configuration

#### [NEW] [pom.xml](file:///d:/Antigravity%20IDE/workspaces/test/security/pom.xml)
- Spring Boot Starter Web, Security, Data JPA, Validation.
- `io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson` (0.12.6).
- `org.springdoc:springdoc-openapi-starter-webmvc-ui`.
- MySQL database added.
- Java compiler settings targeting modern Java baseline.

#### [NEW] `application.yml`
- Server port (`8082`), MySQL datasource (`payment_gateway`), and JPA configuration.
- `app.jwt.secret`, `app.jwt.expiration-ms` (e.g. 900,000 = 15m), `app.jwt.refresh-expiration-ms` (e.g. 604,800,000 = 7 days).

---

### 2. Domain & Data Access Layer

#### [NEW] [BaseEntity.java](file:///d:/Antigravity%20IDE/workspaces/test/security/src/main/java/com/example/security/entity/BaseEntity.java)
- `@MappedSuperclass` with `createdAt`, `updatedAt` managed via `@CreatedDate`, `@LastModifiedDate`.

#### [NEW] [ERole.java](file:///d:/Antigravity%20IDE/workspaces/test/security/src/main/java/com/example/security/entity/ERole.java) & [Role.java](file:///d:/Antigravity%20IDE/workspaces/test/security/src/main/java/com/example/security/entity/Role.java)
- Enum for roles (`ROLE_USER`, `ROLE_MODERATOR`, `ROLE_ADMIN`) and Role entity.

#### [NEW] [User.java](file:///d:/Antigravity%20IDE/workspaces/test/security/src/main/java/com/example/security/entity/User.java)
- User entity with email, username, password hash, account flags (`enabled`, `accountNonLocked`), Many-to-Many roles relationship.

#### [NEW] [RefreshToken.java](file:///d:/Antigravity%20IDE/workspaces/test/security/src/main/java/com/example/security/entity/RefreshToken.java)
- Refresh token persistence with expiry timestamp, revoked flag, and User association.

#### [NEW] Repositories
- [UserRepository.java](file:///d:/Antigravity%20IDE/workspaces/test/security/src/main/java/com/example/security/repository/UserRepository.java): `findByUsername`, `findByEmail`, `existsByUsername`, `existsByEmail`.
- [RoleRepository.java](file:///d:/Antigravity%20IDE/workspaces/test/security/src/main/java/com/example/security/repository/RoleRepository.java): `findByName`.
- [RefreshTokenRepository.java](file:///d:/Antigravity%20IDE/workspaces/test/security/src/main/java/com/example/security/repository/RefreshTokenRepository.java): `findByToken`, `deleteByUser`.

---

### 3. Security & JWT Engine

#### [NEW] [JwtTokenProvider.java](file:///d:/Antigravity%20IDE/workspaces/test/security/src/main/java/com/example/security/security/jwt/JwtTokenProvider.java)
- Modern JJWT 0.12.x API implementation:
    - Token creation with subject, roles claim, issue date, expiration date, and HMAC-SHA256 signature.
    - Safe token validation and claims parsing.
    - Handling specific JWT exceptions (`ExpiredJwtException`, `MalformedJwtException`, `SignatureException`, etc.).

#### [NEW] [JwtAuthenticationFilter.java](file:///d:/Antigravity%20IDE/workspaces/test/security/src/main/java/com/example/security/security/jwt/JwtAuthenticationFilter.java)
- HTTP Bearer extraction from `Authorization` header, validation via `JwtTokenProvider`, and population of `SecurityContextHolder`.

#### [NEW] [JwtAuthEntryPoint.java](file:///d:/Antigravity%20IDE/workspaces/test/security/src/main/java/com/example/security/security/jwt/JwtAuthEntryPoint.java) & [JwtAccessDeniedHandler.java](file:///d:/Antigravity%20IDE/workspaces/test/security/src/main/java/com/example/security/security/JwtAccessDeniedHandler.java)
- Return clean, uniform JSON responses for 401 Unauthorized and 403 Forbidden errors instead of default HTML error pages.

#### [NEW] [SecurityConfig.java](file:///d:/Antigravity%20IDE/workspaces/test/security/src/main/java/com/example/security/config/SecurityConfig.java)
- Configures `SecurityFilterChain` bean:
    - Disables CSRF, sets session policy to `STATELESS`.
    - Configures CORS rules.
    - Defines public vs protected endpoints.
    - Adds `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`.
    - Configures `AuthenticationProvider` (`DaoAuthenticationProvider`) and `BCryptPasswordEncoder`.
    - Configures `AuthenticationManager` bean.

---

### 4. Service & Controller Layer

DTO records include:
    - `RegisterRequest(username, email, password, requestAdminAccess, adminAccessReason, roles)` with Jakarta Validation annotations. Registration always assigns `ROLE_USER`; the client-supplied `roles` field does not grant privileges.
    - `LoginRequest(usernameOrEmail, password)`
    - `ChangePasswordRequest(currentPassword, newPassword)`
    - `TokenRefreshRequest(refreshToken)`
    - `AuthResponse(accessToken, refreshToken, tokenType, expiresIn, id, username, email, roles)`
    - `TokenRefreshResponse(accessToken, refreshToken, tokenType)`
    - `UserProfileResponse(id, username, email, roles, createdAt)`
    - `ApiResponse<T>` & `ErrorResponse`

#### [NEW] [AuthenticationService.java](file:///d:/Antigravity%20IDE/workspaces/test/security/src/main/java/com/example/security/service/AuthenticationService.java) & [RefreshTokenService.java](file:///d:/Antigravity%20IDE/workspaces/test/security/src/main/java/com/example/security/service/RefreshTokenService.java)
- Business logic for user registration, login credential validation, refresh token rotation, and secure revocation.

#### [NEW] Controllers
- `AuthenticationController`: `/api/v1/auth/register`, `/api/v1/auth/login`, authenticated `/api/v1/auth/change-password`, `/api/v1/auth/refresh-token`, `/api/v1/auth/logout`, and machine-client registration/token routes.
- [UserController.java](file:///d:/Antigravity%20IDE/workspaces/test/security/src/main/java/com/example/security/controller/UserController.java): `/api/v1/users/me`, `/api/v1/users/public`.
- `UserController` also supports authenticated administrator-access request submission and status lookup.
- `AdminController`: `/api/v1/admin/dashboard`, `/api/v1/admin/users`, and administrator-access request review endpoints.

#### [NEW] [GlobalExceptionHandler.java](file:///d:/Antigravity%20IDE/workspaces/test/security/src/main/java/com/example/security/exception/GlobalExceptionHandler.java)
- Standardized REST error responses for validation failures, bad credentials, expired/revoked refresh tokens, missing resources (`ResourceNotFoundException`), resource conflicts, and access denials.

---

## Verification Plan

### Automated / Manual Verification
1. **Source Inspection & Code Integrity**: Verify complete project tree, all imports, annotations, and clean record syntax.
2. **Endpoint Flow Verification**:
    - `POST /api/v1/auth/register` -> verify `ROLE_USER` account creation, including optional administrator-access request.
    - `POST /api/v1/auth/change-password` with a bearer token -> verify password change and refresh-token invalidation.
    - `POST /api/v1/auth/login` -> verify receipt of JWT Access Token and Refresh Token.
    - `GET /api/v1/users/me` with `Bearer <accessToken>` -> verify 200 OK and user profile data.
    - `GET /api/v1/admin/dashboard` with regular user token -> verify 403 Forbidden.
    - `GET /api/v1/admin/dashboard` with admin token -> verify 200 OK.
    - `POST /api/v1/auth/refresh-token` -> verify new Access Token and rotated Refresh Token.
    - `POST /api/v1/auth/logout` -> verify token invalidation.


# **Architecture**

┌─────────────┐
│   Client    │
└──────┬──────┘
│ HTTP Request + Bearer Token
▼
┌─────────────────────────────────────┐
│  JWT Authentication Filter          │ ← Validates JWT in Authorization header
└──────┬──────────────────────────────┘
│
▼
┌─────────────────────────────────────┐
│  Security Context & Authentication  │ ← Sets authenticated user
└──────┬──────────────────────────────┘
│
▼
┌─────────────────────────────────────┐
│  Protected REST Endpoints           │ ← Application logic
└─────────────────────────────────────┘

## Administrator access requests

Public registration always creates a `ROLE_USER` account. To request administrator access during registration, set `requestAdminAccess` to `true` and provide an `adminAccessReason`. A submitted `roles` value is ignored for authorization; users cannot self-assign administrator privileges.

The service uses Hibernate `ddl-auto: update`, which creates the `admin_access_requests` table if it is missing. No separate SQL script is required for local startup. For production deployments, use a controlled database migration instead of relying on automatic schema updates.

- `POST /api/v1/auth/register` accepts `username`, `email`, `password`, and optionally `requestAdminAccess` plus `adminAccessReason`. Example:

  ```json
  {
    "username": "newuser",
    "email": "newuser@example.com",
    "password": "StrongPassword1!",
    "requestAdminAccess": true,
    "adminAccessReason": "I need to manage user access."
  }
  ```

- Signed-in users can submit a request with `POST /api/v1/users/me/admin-access-requests` and check its latest status with `GET /api/v1/users/me/admin-access-request`.
- Administrators can review the pending queue with `GET /api/v1/admin/admin-access-requests` and approve or reject with `POST /api/v1/admin/admin-access-requests/{requestId}/decision`, passing `{"status":"APPROVED"}` or `{"status":"REJECTED"}`. An optional `note` can accompany the decision.

Only an approved request adds `ROLE_ADMIN`; the review routes are restricted to administrators. Users can change their password through authenticated `POST /api/v1/auth/change-password`, sending `currentPassword` and `newPassword`; existing refresh tokens are invalidated.

For the service architecture, complete endpoint list, and Postman-ready request examples, see [Auth Architecture and Postman Guide](./AUTH_ARCHITECTURE_AND_POSTMAN_GUIDE.md).

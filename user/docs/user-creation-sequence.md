# User Registration Sequence Diagram

User registration is currently handled by the Auth Service. The User Service owns user-management APIs, but it does not currently expose a user-creation endpoint.

```mermaid
sequenceDiagram
    autonumber

    actor Client
    participant AuthController as Auth Service<br/>AuthenticationController
    participant AuthService as Auth Service<br/>AuthenticationService
    participant UserRepo as Auth Service<br/>UserRepository
    participant RoleRepo as Auth Service<br/>RoleRepository
    participant Encoder as PasswordEncoder
    participant AuthDB as MySQL<br/>payment_gateway

    Client->>AuthController: POST /api/v1/auth/register<br/>{username, email, password, requestAdminAccess?, adminAccessReason?}
    AuthController->>AuthController: Validate RegisterRequest

    alt Request validation fails
        AuthController-->>Client: 400 Bad Request
    else Request is valid
        AuthController->>AuthService: register(registerRequest)

        AuthService->>UserRepo: existsByUsername(username)
        UserRepo->>AuthDB: Query username
        AuthDB-->>UserRepo: Exists result
        UserRepo-->>AuthService: true / false

        alt Username already exists
            AuthService-->>AuthController: UserAlreadyExistsException
            AuthController-->>Client: 409 Conflict / error response
        else Username is available
            AuthService->>UserRepo: existsByEmail(email)
            UserRepo->>AuthDB: Query email
            AuthDB-->>UserRepo: Exists result
            UserRepo-->>AuthService: true / false

            alt Email already exists
                AuthService-->>AuthController: UserAlreadyExistsException
                AuthController-->>Client: 409 Conflict / error response
            else Email is available
                AuthService->>Encoder: encode(password)
                Encoder-->>AuthService: Encoded password

                AuthService->>RoleRepo: findByName(ROLE_USER)
                RoleRepo->>AuthDB: Query default role
                AuthDB-->>RoleRepo: ROLE_USER
                RoleRepo-->>AuthService: Default role

                AuthService->>UserRepo: save(new JwtUser)
                UserRepo->>AuthDB: INSERT user and user_roles
                AuthDB-->>UserRepo: Persisted user
                UserRepo-->>AuthService: Saved user
                opt requestAdminAccess is true
                    AuthService->>AuthService: Create pending admin access request
                    AuthService->>AuthDB: INSERT admin_access_requests
                end
                AuthService-->>AuthController: Registration result
                AuthController-->>Client: 201 Created<br/>Success; pending-review message if requested
            end
        end
    end
```

## Participants

| Participant | Responsibility |
|---|---|
| Client | Sends registration details. |
| `AuthenticationController` | Exposes `POST /api/v1/auth/register` and validates the request. |
| `AuthenticationService` | Checks uniqueness, encodes the password, always assigns `ROLE_USER`, saves the user, and optionally creates an administrator-access request. |
| `UserRepository` | Checks and persists users. |
| `RoleRepository` | Loads the required `ROLE_USER`; client-supplied role names do not determine granted privileges. |
| `AdminAccessRequestService` | Creates a pending administrator-access request when the registration payload asks for one. |
| `PasswordEncoder` | Stores only the encoded password. |
| MySQL | Persists the user, roles, user-role relationship, and optional administrator-access request. |

## Important behavior

- Username and email uniqueness are checked before persistence.
- Email is trimmed and normalized to lowercase.
- Passwords are encoded before being saved.
- Every public registration receives only `ROLE_USER`, even if a client includes a `roles` field.
- `requestAdminAccess: true` requires an `adminAccessReason`; the request remains pending until an existing administrator approves it.
- Hibernate is configured with `ddl-auto: update` in the Auth Service and creates the administrator-access request table if it is missing. No manual SQL script is required for local startup.
- Missing configured `ROLE_USER` results in a resource-not-found error.

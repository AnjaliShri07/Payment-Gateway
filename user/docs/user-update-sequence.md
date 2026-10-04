# User Update Sequence Diagram

The update endpoint supports partial updates. Only non-null fields supplied in `UserUpdateRequest` are copied to the existing user entity.

```mermaid
sequenceDiagram
    autonumber

    actor Client
    participant Filter as User Service<br/>TokenAuthenticationFilter
    participant AuthClient as AuthenticationServiceClient
    participant Auth as Auth Service
    participant Controller as UserController
    participant Service as UserServiceImpl
    participant Repo as UserRepository
    participant DB as MySQL<br/>payment_gateway

    Client->>Filter: PUT /api/v1/users/{id}<br/>Authorization: Bearer JWT<br/>partial JSON body

    Filter->>AuthClient: getAuthenticatedUser(bearerToken)
    AuthClient->>Auth: GET /api/v1/users/me<br/>Authorization: Bearer JWT
    Auth-->>AuthClient: AuthApiResponse<AuthUserProfile>

    alt Token is invalid or Auth Service is unavailable
        AuthClient-->>Filter: Optional.empty()
        Filter->>Filter: Do not populate SecurityContext
        Filter->>Controller: Continue filter chain
        Controller-->>Client: 401 Unauthorized
    else Token is valid
        AuthClient-->>Filter: Optional<AuthUserProfile>
        Filter->>Filter: Create authentication with profile roles
        Filter->>Controller: Continue filter chain

        Controller->>Controller: Bind and validate UserUpdateRequest

        alt Request validation fails
            Controller-->>Client: 400 Bad Request<br/>field validation errors
        else Request is valid
            Controller->>AuthClient: getAuthenticatedUser(bearerToken)
            AuthClient->>Auth: GET /api/v1/users/me
            Auth-->>AuthClient: AuthUserProfile
            AuthClient-->>Controller: Optional<AuthUserProfile>

            Controller->>Service: update(id, updateRequest)
            Service->>Repo: findById(id)
            Repo->>DB: SELECT user by ID
            DB-->>Repo: User or no result
            Repo-->>Service: Optional<User>

            alt User does not exist
                Service-->>Controller: null
                Controller-->>Client: 404 Not Found<br/>"User not found"
            else User exists
                Service->>Service: Copy only non-null fields

                Note over Service: Omitted fields remain unchanged.<br/>Boolean fields use nullable Boolean values<br/>to distinguish omitted from false.

                Service->>Repo: save(existingUser)
                Repo->>DB: UPDATE users and user_roles
                DB-->>Repo: Updated user
                Repo-->>Service: Saved user
                Service-->>Controller: Updated user
                Controller-->>Client: 200 OK<br/>BaseResponse<User>
            end
        end
    end
```

## Request example

```http
PUT /api/v1/users/42
Authorization: Bearer <JWT>
Content-Type: application/json
```

```json
{
  "email": "new-address@example.com",
  "enabled": false
}
```

The service changes only `email` and `enabled`. Existing username, password, roles, and other account-status fields are preserved.

## Main implementation responsibilities

| Component | Responsibility |
|---|---|
| `TokenAuthenticationFilter` | Validates the bearer token and establishes the security context. |
| `AuthenticationServiceClient` | Calls the Auth Service and returns the authenticated profile. |
| `UserController` | Binds the request, validates the DTO, and maps the result to HTTP status and response body. |
| `UserServiceImpl` | Loads the existing entity, applies only supplied fields, and saves it. |
| `UserRepository` | Reads and persists the user entity. |
| MySQL | Stores the updated user and role association data. |

## Current implementation note

The current request path validates the token in the security filter and performs another profile lookup inside `UserController.update`. This is represented in the diagram because it is the current behavior; the controller-level lookup can later be removed if the authenticated principal from the security context is used directly.

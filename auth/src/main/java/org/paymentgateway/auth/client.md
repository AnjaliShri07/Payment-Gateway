Demonstrating dynamic admin-flow (curl + JSON). Replace host/port and placeholder values.

1) Admin login — get ADMIN_TOKEN
curl -s -X POST "http://localhost:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"admin@example.com","password":"adminpass"}'

Expected (excerpt):
{
  "success": true,
  "message": "Login successful!",
  "data": {
    "accessToken": "eyJhbGciOiJI...",
    "refreshToken": "...",
    "tokenType":"Bearer",
    ...
  }
}
Use data.accessToken as ADMIN_TOKEN.

2) Register client (admin-only) — returns clientId + clientSecret (shown once)
curl -s -X POST "http://localhost:8081/api/v1/auth/register-client" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -d '{"name":"payments-service","scopes":["payments.read","payments.write"]}'

Response (excerpt):
{
  "success": true,
  "message": "Client registered successfully!",
  "data": {
    "clientId": "client-3f9a2c4b-...",
    "clientSecret": "YzFf...AbCd"    <-- SAVE THIS NOW
  }
}

Important: store clientSecret securely now. It is not retrievable later.

3) Obtain machine token (client_credentials)
curl -s -X POST "http://localhost:8081/api/v1/auth/token" \
  -H "Content-Type: application/json" \
  -d '{"clientId":"client-3f9a2c4b-...","clientSecret":"YzFf...AbCd","grantType":"client_credentials"}'

Response (excerpt):
{
  "success": true,
  "message": "Token issued successfully!",
  "data": {
    "accessToken":"eyJhbGciOiJI...","tokenType":"Bearer","expiresIn":900,"scope":"payments.read payments.write"
  }
}
Use data.accessToken as MACHINE_TOKEN.

4) Call protected API using machine token
curl -s -X GET "http://localhost:8080/api/v1/payments" \
  -H "Authorization: Bearer MACHINE_TOKEN"

Notes and best practices
- Protect /register-client (admin only). Demo assumes admin user exists.
- clientSecret is returned plaintext only once — store it in a vault or env.
- Token TTL = jwt expiration (default 900 seconds / 15 minutes). Refresh/rotate as needed.
- For production, add rate limiting, revoke/rotate secrets, and prefer confidential authentication (mTLS or vault-backed client secrets).
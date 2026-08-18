# Payment-Gateway

**User-auth Implementation Flow**
┌─────────────────────────────────────────┐
│         Client Application              │
└────────────────────┬────────────────────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
        ▼            ▼            ▼
    [Register]  [Login]      [Logout]
        │            │            │
        └────────────┴────────────┘
                     │
        ┌────────────┴────────────┐
        ▼                         ▼
   USER SERVICE             AUTH SERVICE
   ├─ Create user           ├─ Generate JWT
   ├─ Store credentials     ├─ Validate token
   └─ User profile          └─ Manage sessions

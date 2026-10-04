# GlobePayHub UI Flow

This guide describes the user-facing flows in the Angular application. The UI is
implemented as a single workspace component; authenticated views are selected
using client-side view state rather than separate URL routes.

## Entry and authentication

When the app opens, it checks `sessionStorage` for an access token:

- If no token exists, the user sees the Login / Sign up screen.
- If a token exists, the app loads the current profile. A successful response
  restores the workspace and loads payments and administrator-request status.
- If the profile request returns `401 Unauthorized`, the local session is
  cleared and the user must sign in again. Other profile errors are displayed
  without discarding the session.

### Sign up

1. The user enters a username, email, and password.
2. Optionally, the user selects **Also request administrator access** and
   provides a reason.
3. The UI submits the registration to the authentication service.
4. On success, the UI switches to Login and pre-fills the email address.

Registration always creates a standard user account. Requesting administrator
access creates a pending request; it does not grant an administrator role.

### Login

1. The user signs in with an email address or username and password.
2. On success, the UI stores the access and refresh tokens in
   `sessionStorage`, loads the user's payments and access-request status, and
   opens Overview.
3. Login errors and request timeouts are shown in the UI.

The UI currently does not automatically refresh an expired access token. A
`401` response from profile loading clears the local session and requires a new
login.

## Authenticated workspace

The sidebar provides these views:

| View | Availability | Purpose |
| --- | --- | --- |
| Dashboard | All signed-in users | Shows account status, completed payment count and volume, and recent payments. Provides shortcuts to Analytics and Payments. |
| Analytics | All signed-in users | Loads the signed-in user's transaction totals, completion and decline rates, processed volume, and payment status breakdown from the payment service. |
| Payments | All signed-in users | Lists the user's transactions and provides the payment form. Selecting a transaction opens its details. |
| Transaction details | All signed-in users | Loads an individual transaction by ID and shows payment, processing, and refund information where available. Only masked card digits are displayed. |
| My profile | All signed-in users | Displays account details, permits editing username/email, and shows administrator-request status and controls. |
| Security | All signed-in users | Changes the account password. On success, the UI clears the session and prompts the user to sign in again. |
| All users | Administrators only | Searches the user directory and reviews pending administrator-access requests. |

Requests show a loading state where applicable. API failures are reported in
the UI; non-authentication profile failures do not clear the session.

## Payment flow

1. The user enters an amount, selects a currency (INR, USD, or EUR) and card
   type (debit or credit), and provides card details. The amount prefix changes
   with the selected currency.
2. The UI sends card details to the payment service's tokenization endpoint.
3. If tokenization succeeds, the UI submits the returned card token with the
   payment amount, currency, description, and card type.
4. On success, the transaction is added to the visible payment list and the
   form is reset. The transaction's returned status and reference are shown.
5. Validation errors, authentication failures, service errors, and timeouts are
   surfaced to the user.

The payment endpoints require authentication. Card details are submitted for
tokenization before the payment request; the payment submission uses the
resulting token.

## Administrator-access request and review

### User request

A signed-in standard user can submit a reason from My profile. The UI displays
the current request status:

- **Pending**: the request is awaiting an administrator; another request cannot
  be submitted from this screen.
- **Approved**: the user is instructed to sign out and back in to refresh their
  administrator access.
- **Rejected**: the user can submit a new request with a reason.

### Administrator review

Users with the administrator role see the All users view. It contains the
registered-user directory and pending access requests. An administrator can
approve or reject each pending request. Approval grants administrator access;
rejection does not change the requester's role.

## Sign out

Sign out immediately clears tokens and user data from the browser session. If a
refresh token is present, the UI also asks the authentication service to
invalidate it. If server-side logout fails, the UI reports that the user has
been signed out locally but that server logout could not be confirmed.

## Service endpoints used by the UI

The development UI calls these services directly:

| Service | Base URL | Main operations |
| --- | --- | --- |
| Authentication | `http://localhost:8082/api/v1` | Registration, login, profile, password change, logout, and administrator-access requests/reviews. |
| User | `http://localhost:8081/api/v1` | Updating the signed-in user's profile. |
| Payment | `http://localhost:8083/api/v1` | Listing payments, retrieving per-user analytics and transaction details, tokenizing cards, and submitting payments. |

These base URLs are currently constants in the frontend application component.

# Client JWT & Session Integration Guide

This guide provides frontend and mobile developers with the exact client integration patterns required to interact with QuickBite's **Dual-JWT & Stateful Session Authentication Architecture**.

---

## 1. Token Specification & Scopes

QuickBite issues two distinct cryptographic JWTs signed using RSA256, segregated by the OAuth 2.0 `aud` (Audience) claim.

| Token Type | Audience (`aud`) | Granted Scope | Typical Lifetime | Transport Channel | Intended Purpose |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **API Access Token** | `quickbite-api` | `SCOPE_API` + `ROLE_*` | 15 Minutes (`PT15M`) | `Authorization: Bearer <token>` Header | Standard business APIs (`/api/v1/customer/**`, `/api/v1/restaurant-owner/**`, etc.) |
| **Challenge Token** | `quickbite-auth` | `SCOPE_AUTH` | 5 Minutes (`PT5M`) | `Authorization: Bearer <token>` Header | Evicting remote sessions when concurrent limit is reached (`/api/v1/auth/sessions/**`) |
| **Refresh Token** | N/A (Opaque) | N/A | 7 Days (`P7D`) | `qb_refresh_token` Cookie (`HttpOnly`, `Secure`, `SameSite=Strict`) | Rotating access tokens via `POST /api/v1/auth/refresh-token` |

---

## 2. Standard Client Request Flow

### 2.1 Making Authenticated API Calls

Always pass the Access Token in the HTTP `Authorization` header:

```bash
curl -X GET http://localhost:8080/api/v1/customer/me \
     -H "Authorization: Bearer <ACCESS_TOKEN>" \
     -H "Content-Type: application/json"
```

### 2.2 Optional Client Context Header
To assist in session tracking and device classification, clients may provide the optional `X-Client-Type` header:
* `WEB`
* `MOBILE_APP`
* `TABLET`
* `DESKTOP`

```http
GET /api/v1/customer/cart HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
X-Client-Type: MOBILE_APP
```

---

## 3. Resolving the `409 Conflict` Session Challenge

QuickBite limits each user to **3 concurrent active sessions** (`quickbite.auth.max-concurrent-sessions = 3`).

When a user attempts to log in from a 4th device, the login endpoint **rejects the attempt with HTTP 409 Conflict** and provides a short-lived **Challenge Token**.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Client as Web / Mobile App
    participant Auth as QuickBite Auth Server

    User->>Client: Enter Email & Password
    Client->>Auth: POST /api/v1/auth/login
    Auth-->>Client: HTTP 409 Conflict (SessionLimitErrorResponse)
    Note over Client: Payload contains challengeToken (5m TTL)<br/>and activeSessions list

    Client->>User: Display Active Sessions Modal
    User->>Client: Selects an older session to evict
    Client->>Auth: DELETE /api/v1/auth/sessions/{sessionId}<br/>(Authorization: Bearer <challengeToken>)
    Auth-->>Client: HTTP 204 No Content

    Client->>Auth: POST /api/v1/auth/claim-session<br/>(Authorization: Bearer <challengeToken>)
    Auth-->>Client: HTTP 200 OK (AuthResponse + Set-Cookie: qb_refresh_token)
    Note over Client: User successfully logged in!
```

### Step-by-Step Resolution

#### 1. Receive 409 Response on Login
```json
{
  "errorCode": "MAX_SESSIONS_EXCEEDED",
  "message": "Maximum active session limit (3) exceeded.",
  "challengeToken": "eyJhbGciOiJSUzI1NiIs...",
  "activeSessions": [
    {
      "id": "018f3a54-726e-7000-848e-28b3a0e69b51",
      "clientType": "DESKTOP",
      "ipAddress": "192.168.1.10",
      "userAgent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)...",
      "createdAt": "2026-10-14T10:00:00Z",
      "lastAccessedAt": "2026-10-15T09:30:00Z"
    }
  ]
}
```

#### 2. Evict Remote Session
Pass the `challengeToken` as a Bearer token:
```bash
curl -X DELETE http://localhost:8080/api/v1/auth/sessions/018f3a54-726e-7000-848e-28b3a0e69b51 \
     -H "Authorization: Bearer <CHALLENGE_TOKEN>"
```

#### 3. Claim New Session
```bash
curl -X POST http://localhost:8080/api/v1/auth/claim-session \
     -H "Authorization: Bearer <CHALLENGE_TOKEN>"
```
Returns a standard `AuthResponse` containing the new `accessToken` and issues the `qb_refresh_token` cookie.

---

## 4. Token Rotation & Refresh

When the Access Token expires (HTTP `401 Unauthorized`), the client calls the refresh endpoint.

### 4.1 Browser / Frontend JavaScript Example (`fetch`)

Because the refresh token resides in an `HttpOnly` cookie, browser requests must specify `credentials: 'include'`:

```javascript
async function refreshAccessToken() {
  const response = await fetch('/api/v1/auth/refresh-token', {
    method: 'POST',
    credentials: 'include', // Sends the qb_refresh_token cookie automatically
    headers: {
      'Content-Type': 'application/json'
    }
  });

  if (!response.ok) {
    // Session expired or revoked; redirect to login
    window.location.href = '/login';
    return null;
  }

  const data = await response.json();
  return data.accessToken; // New 15-minute access token
}
```

### 4.2 Concurrent Tab Refresh Grace Window
If a user has multiple tabs open and all tabs simultaneously call `/refresh-token` when the access token expires, QuickBite's **2-second Redis rotation grace cache** ensures that concurrent requests receive the same newly rotated token rather than triggering a token-reuse security breach.

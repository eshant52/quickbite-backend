# QuickBite Authentication & Session Management Architecture

This document provides a comprehensive architectural analysis of the `com.quickbite.quickbite.auth` package and its supporting security infrastructure in QuickBite.

---

## 1. Executive Summary & Core Philosophy

QuickBite implements a **Hybrid Stateful-Session / Stateless-Token Authentication System** built on **Spring Boot 4.1.0**, **Java 25**, **Spring Security 6+**, **PostgreSQL 18**, and **Redis 8 / Redisson**.

### Architectural Pillars
* **Self-Issued Asymmetric JWTs**: Acts as its own OAuth 2.0 Authorization Server and Resource Server using 2048-bit RSA key pairs (`private.pem` and `public.pem`), supporting both local resource URIs and raw PEM injection from AWS Secrets Manager.
* **Dual JWT Segregation**: Strictly isolates business API access (`aud: quickbite-api`, `type: access`, 15m TTL) from concurrent session challenge resolution (`aud: quickbite-auth`, `type: session-limit`, 5m TTL).
* **Client-Aware Hybrid Refresh Token Transport**:
  * **`WEB_BROWSER` (`X-Client-Type: web_browser`)**: Refresh tokens are transported exclusively via `HttpOnly`, `Secure`, `SameSite=Strict` cookies (`qb_refresh_token`) and stripped (`null`) from JSON response bodies to neutralize XSS token theft.
  * **`MOBILE_APP` (`X-Client-Type: mobile_app`)**: Refresh tokens are returned directly in the `AuthResponse` JSON payload and accepted via the `RefreshRequest` JSON body so native apps can store them in iOS Keychain or Android Keystore.
* **Generation-Tracked Token Families & Breach Detection**: Refresh tokens belong to a `RefreshTokenFamily` tied to a `Session` with monotonically increasing `generation` numbers (`1, 2, 3...`). Attempting to reuse an already-rotated token (`used_at != null`) outside the 2-second grace window triggers a security breach protocol, revoking the family (`reuse_detected_at`) and session in an independent transaction (`Propagation.REQUIRES_NEW`).
* **Authoritative Persistence & Distributed Concurrency**: Active session counts and states are authoritatively enforced in **PostgreSQL**, coordinated by **Redisson** distributed locks (`RLock`) per user (for session creation) and per token hash (for double-checked rotation), alongside a **2-second Redis grace cache** for concurrent tab/network retries.
* **Bounded Storage via Automated Cleanup**: `RefreshTokenCleanupScheduler` purges used, expired, or revoked token records older than a 30-day retention window, preventing unbounded table growth while preserving recent history for breach detection.

---

## 2. Package & Component Structure

The `com.quickbite.quickbite.auth` domain follows strict **Interface Segregation (ISP)** and **Adapter Pattern** boundaries:

```
com.quickbite.quickbite.auth
├── controller
│   ├── AuthenticationController.java            # POST /login and POST /refresh-token (Web Cookie vs Mobile Body)
│   ├── LogoutController.java                    # POST /logout and POST /logout-all (Requires SCOPE_API)
│   ├── SessionChallengeController.java          # GET /sessions, DELETE /sessions/{id}, POST /claim-session (Requires SCOPE_AUTH)
│   └── UserRegistrationController.java          # POST /register/{customer, delivery-agent, restaurant-owner}
├── dto
│   ├── AuthResponse.java                        # Response record (accessToken, refreshToken, expiresIn, sessionId, tokenType)
│   ├── AuthenticatedSession.java                # Resolved principal tuple (userId, sessionId)
│   ├── ClientRequestMetadata.java               # Raw request inputs (userAgent, clientType, ipAddress)
│   ├── DeviceInfo.java                          # Parsed metadata (deviceName, deviceOs, osVersion, clientType, ip, userAgent)
│   ├── IssuedToken.java                         # Internal session/token tuple (rawToken, sessionId, userId)
│   ├── LoginRequest.java                        # Email & password credentials
│   ├── RefreshRequest.java                      # Mobile refresh request body (refreshToken)
│   ├── RegisterRequest.java                     # Registration payload with JSON/XSS escaping
│   ├── SessionLimitErrorResponse.java           # HTTP 409 payload (error, challengeToken, maxSessions)
│   └── SessionResponse.java                     # Public session projection (sessionId, deviceName, deviceOS, daysLeft, etc.)
├── exception
│   ├── AuthenticationException.java             # Domain auth failure (mapped to HTTP 401 ProblemDetail)
│   └── MaxSessionException.java                 # Concurrent session limit breach (mapped to HTTP 409)
├── model
│   ├── ClientType.java                          # PostgreSQL Enum (WEB_BROWSER, MOBILE_APP)
│   ├── RefreshToken.java                        # Hashed token entity with generation counter & usedAt/revokedAt timestamps
│   ├── RefreshTokenFamily.java                  # Lineage aggregate root tracking session rotation & reuse breaches
│   └── Session.java                             # Active device session entity
├── repository
│   ├── RefreshTokenFamilyRepository.java        # JPQL bulk revocation & breach updates
│   ├── RefreshTokenRepository.java              # Eager family/session fetch, atomic markTokenUsed, & stale token purge
│   └── SessionRepository.java                   # Active session counting, listing, lastUsed updates, & user-scoped revocation
├── scheduler
│   └── RefreshTokenCleanupScheduler.java        # Daily cron (03:00 AM) purging stale tokens older than 30 days
├── service
│   ├── AuthCookieService.java                   # Cookie management contract
│   ├── AuthCookieServiceImpl.java               # Builds HttpOnly, Secure, SameSite=Strict ResponseCookies
│   ├── AuthenticationService.java               # Narrow interface for login & refresh
│   ├── LogoutService.java                       # Narrow interface for single & global session logout
│   ├── SessionChallengeService.java             # Narrow interface for SCOPE_AUTH session challenge workflows
│   ├── AuthServiceImpl.java                     # Implements AuthenticationService, SessionChallengeService, LogoutService
│   ├── UserRegistrationService.java             # Narrow interface for multi-role user registration
│   ├── UserRegistrationServiceImpl.java         # Validates email/phone uniqueness, Argon2 hashes password, saves User
│   ├── SessionService.java                      # Session & token lifecycle contract
│   ├── SessionServiceImpl.java                  # Redisson lock coordinator & double-checked grace cache manager
│   ├── SessionPersistenceService.java           # Transactional DB worker contract
│   ├── SessionPersistenceServiceImpl.java       # @Transactional PostgreSQL persistence & REQUIRES_NEW breach handler
│   ├── SessionStoreService.java                 # Volatile grace cache abstraction
│   ├── SessionRedisStoreService.java            # Redis implementation (quickbite:token-grace:<hash>)
│   └── token
│       ├── AccessTokenService.java              # Mints & verifies API Access Tokens (aud: quickbite-api, type: access)
│       ├── ChallengeTokenService.java           # Mints & verifies Challenge Tokens (aud: quickbite-auth, type: session-limit)
│       ├── SpringSecurityTokenService.java      # Bridges TokenService with Nimbus JwtEncoder / JwtDecoder
│       └── TokenService.java                    # Low-level JWT claim encoding & decoding contract
└── util
    ├── AuthenticatedSessionResolver.java        # Contract for extracting userId & sessionId from Spring Security Jwt
    ├── AuthenticatedSessionResolverImpl.java    # Validates sub and session_id claims
    ├── DeviceInfoResolver.java                  # Adapter interface for device/user-agent resolution
    ├── YauaaDeviceInfoResolver.java             # YAUAA implementation backed by a 10,000-entry Caffeine W-TinyLFU cache
    └── TokenUtils.java                          # 64-byte SecureRandom generator & SHA-256 Base64 hasher
```

---

## 3. Domain Entity & Data Model

The persistence model uses **timestamp-based state tracking** (`used_at`, `revoked_at`, `reuse_detected_at`, `expires_at`) rather than destructive updates or simple booleans, preserving a complete forensic audit trail. All entities extend `common.model.Base` (UUIDv7 primary keys, `created_at`, `updated_at`).

```mermaid
erDiagram
    USERS ||--o{ SESSIONS : owns
    SESSIONS ||--o{ REFRESH_TOKEN_FAMILIES : tracks
    REFRESH_TOKEN_FAMILIES ||--o{ REFRESH_TOKENS : contains

    USERS {
        uuid id PK
        string name
        string email UK
        string phone_number UK
        string password_hash
        user_role role
        boolean is_active
        timestamp last_login_at
        timestamp created_at
        timestamp updated_at
    }

    SESSIONS {
        uuid id PK
        uuid user_id FK
        string device_name
        string device_os
        client_type client_type
        string ip
        text user_agent
        timestamp login_at
        timestamp last_used_at
        timestamp expires_at
        timestamp revoked_at
        timestamp created_at
        timestamp updated_at
    }

    REFRESH_TOKEN_FAMILIES {
        uuid id PK
        uuid session_id FK
        timestamp revoked_at
        timestamp reuse_detected_at
        timestamp created_at
        timestamp updated_at
    }

    REFRESH_TOKENS {
        uuid id PK
        uuid family_id FK
        text token_hash UK
        int generation
        timestamp used_at
        timestamp expires_at
        timestamp revoked_at
        timestamp created_at
        timestamp updated_at
    }
```

### Key Database Invariants & Indexes
1. **Active Session Index (`idx_sessions_user_active`)**: Composite index on `(user_id, revoked_at, expires_at)` accelerates `countActiveByUserId` and `findActiveByUserId` queries (`WHERE user_id = :userId AND revoked_at IS NULL AND expires_at > CURRENT_TIMESTAMP`).
2. **Unique Token Hash**: `refresh_tokens.token_hash` has a unique constraint and index, enabling single-query `JOIN FETCH` lookup of `RefreshToken` + `RefreshTokenFamily` + `Session` + `User`.
3. **Atomic Single-Use Transition**: `RefreshTokenRepository.markTokenUsed` executes `UPDATE RefreshToken rt SET rt.usedAt = :now WHERE rt.id = :id AND rt.usedAt IS NULL`. An update count of `1` proves first use; `0` proves the token was already consumed (breach).

---

## 4. Token & Security Architecture

### 4.1 Token Specification Matrix

| Attribute | Access Token (AT) | Challenge Token (CT) | Refresh Token (RT) |
| :--- | :--- | :--- | :--- |
| **Format** | Signed JWT (RS256) | Signed JWT (RS256) | 64-Byte `SecureRandom` (Base64) |
| **Default Lifetime** | 15 Minutes (`PT15M`) | 5 Minutes (`PT5M`) | 7 Days (`P7D`) |
| **Audience (`aud`)** | `quickbite-api` | `quickbite-auth` | N/A |
| **Custom `type` Claim** | `"access"` | `"session-limit"` | N/A |
| **Embedded Claims** | `sub` (userId), `session_id`, `email`, `role`, `jti`, `iat`, `exp` | `sub` (userId), `jti`, `iat`, `exp` | N/A (Opaque) |
| **Granted Authorities** | `SCOPE_API` + `ROLE_<ROLE>` | `SCOPE_AUTH` | N/A |
| **Web Browser Transport** | `Authorization: Bearer <AT>` | `Authorization: Bearer <CT>` | Cookie `qb_refresh_token` (`HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/api/v1/auth`) |
| **Mobile App Transport** | `Authorization: Bearer <AT>` | `Authorization: Bearer <CT>` | JSON Body (`AuthResponse.refreshToken` / `RefreshRequest.refreshToken`) |
| **Server Storage** | Stateless | Stateless | PostgreSQL (`SHA-256` hash) + Redis (2s rotation grace cache) |

### 4.2 Client-Aware Transport Branching (`X-Client-Type`)

Every endpoint that issues or consumes a refresh token (`/login`, `/refresh-token`, `/claim-session`) resolves `ClientRequestMetadata` via `YauaaDeviceInfoResolver`:

1. **Header Validation**: Requires `X-Client-Type` header with value `'web_browser'` or `'mobile_app'`. Missing or unrecognized values immediately fail with `400 Bad Request`.
2. **Web Browser (`ClientType.WEB_BROWSER`)**:
   - **Issuance**: Attaches `Set-Cookie: qb_refresh_token=<rawToken>; Path=/api/v1/auth; Max-Age=604800; Secure; HttpOnly; SameSite=Strict` and overwrites `refreshToken` to `null` in the `AuthResponse` JSON body.
   - **Consumption (`/refresh-token`)**: Reads the token exclusively from the `qb_refresh_token` cookie via `AuthCookieService.extractRefreshTokenFromCookie`.
3. **Mobile App (`ClientType.MOBILE_APP`)**:
   - **Issuance**: Returns the full `AuthResponse` JSON containing the raw `refreshToken` field; no cookie header is set.
   - **Consumption (`/refresh-token`)**: Reads the token exclusively from the `@RequestBody RefreshRequest` JSON payload.

### 4.3 Spring Security Request Lifecycle

```mermaid
flowchart TD
    Req["Incoming HTTP Request"] --> Filter["SecurityFilterChain"]
    Filter --> CORS["CorsConfigurationSource Check"]
    CORS --> BearerRes["Custom BearerTokenResolver<br/>(Ignores empty 'Bearer' headers)"]
    BearerRes --> Matcher{"URL Path & Method Matcher"}

    Matcher -->|Public Endpoints| PermitAll["Permit All<br/>(/auth/register/**, /auth/login, /auth/refresh-token, GET catalogs, /webhooks/**)"]
    Matcher -->|Session Challenge Routes| AuthScope["Requires SCOPE_AUTH<br/>(/auth/claim-session, /auth/sessions/**)"]
    Matcher -->|Role-Protected Routes| RoleCheck["Requires ROLE_ADMIN, ROLE_CUSTOMER,<br/>ROLE_RESTAURANT_OWNER, or ROLE_DELIVERY_AGENT"]
    Matcher -->|Fallback Routes| ApiScope["Requires SCOPE_API<br/>(e.g. /auth/logout, /files/upload-url, /notifications/**)"]

    AuthScope & RoleCheck & ApiScope --> JwtDec["NimbusJwtDecoder (RSA Public Key)"]

    JwtDec -->|Validate RS256 Signature & exp| Conv["JwtAuthenticationConverter"]

    Conv --> CheckAud{"Inspect 'aud' Claim"}
    CheckAud -->|aud contains quickbite-auth| GrantAuth["Grant SCOPE_AUTH Only"]
    CheckAud -->|aud contains quickbite-api| GrantApi["Grant SCOPE_API and ROLE_*"]
    CheckAud -->|Unrecognized aud| GrantNone["Grant No Authorities"]

    GrantAuth & GrantApi & GrantNone --> MethodSec["Method Security Check<br/>(@PreAuthorize)"]

    MethodSec -->|Authorized| ExecController["Execute Controller & AuthenticatedSessionResolver"]
    MethodSec -->|Denied| Deny403["403 Forbidden (ProblemDetail)"]
```

---

## 5. End-to-End Sequence Flows

### 5.1 Multi-Role Registration Flow

`UserRegistrationController` exposes dedicated registration endpoints for each non-admin persona:
* `POST /api/v1/auth/register/customer` &rarr; `UserRole.CUSTOMER`
* `POST /api/v1/auth/register/delivery-agent` &rarr; `UserRole.DELIVERY_AGENT`
* `POST /api/v1/auth/register/restaurant-owner` &rarr; `UserRole.RESTAURANT_OWNER`

1. **Sanitization**: `RegisterRequest` escapes JSON/XSS control characters across `name`, `email`, `phoneNumber`, and `password`.
2. **Conflict Checks**: `UserRegistrationServiceImpl` verifies neither `email` nor `phoneNumber` is already registered (throwing `409 Conflict` via `ResourceConflictException` if found).
3. **Password Hashing**: Encodes the password using `Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()` and persists the active `User`.
4. **Admin Provisioning**: Initial super-admin is seeded at boot by `AdminBootstrapRunner` (`quickbite.bootstrap.admin.*`), and subsequent admins are created via `POST /api/v1/admin/users/admins`.

---

### 5.2 User Authentication & Login Flow

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Client as Client (Web or Mobile)
    participant AuthCtrl as AuthenticationController
    participant Resolver as YauaaDeviceInfoResolver
    participant AuthSvc as AuthServiceImpl
    participant SessSvc as SessionServiceImpl
    participant Redis as Redisson Lock
    participant PersistSvc as SessionPersistenceServiceImpl
    participant DB as PostgreSQL

    User->>Client: Submit Credentials (email, password)
    Client->>AuthCtrl: POST /api/v1/auth/login (Header: X-Client-Type)
    AuthCtrl->>Resolver: resolve(ClientRequestMetadata)
    Note over Resolver: Validates X-Client-Type ('web_browser' or 'mobile_app')<br/>& parses User-Agent via Caffeine cache
    Resolver-->>AuthCtrl: DeviceInfo
    AuthCtrl->>AuthSvc: login(LoginRequest, DeviceInfo)

    AuthSvc->>DB: findUserByEmail(email)
    AuthSvc->>AuthSvc: Verify Argon2 password & user.isActive()
    AuthSvc->>SessSvc: createNewSession(user, deviceInfo)

    SessSvc->>Redis: tryLock("quickbite:session-lock:" + userId, 0s, 10s)
    SessSvc->>PersistSvc: persistNewSession(user, deviceInfo, expiresAt, rawToken)
    PersistSvc->>DB: countActiveByUserId(userId)

    alt Active Sessions < maxConcurrentSessions (3)
        PersistSvc->>DB: INSERT Session, RefreshTokenFamily, RefreshToken (gen=1, SHA-256)
        PersistSvc-->>SessSvc: Session
        SessSvc->>Redis: unlock()
        SessSvc-->>AuthSvc: IssuedToken (rawToken, sessionId, userId)
        AuthSvc->>AuthSvc: accessTokenService.generateAccessToken(...)
        AuthSvc->>DB: Update user.lastLoginAt
        AuthSvc-->>AuthCtrl: AuthResponse
        alt ClientType == WEB_BROWSER
            AuthCtrl-->>Client: 200 OK + Set-Cookie: qb_refresh_token (body refreshToken: null)
        else ClientType == MOBILE_APP
            AuthCtrl-->>Client: 200 OK (Full AuthResponse with refreshToken in JSON body)
        end
    else Active Sessions >= maxConcurrentSessions (3)
        PersistSvc->>PersistSvc: challengeTokenService.generateSessionLimitChallenge(userId)
        PersistSvc-->>SessSvc: Throw MaxSessionException(challengeToken, 3)
        SessSvc->>Redis: unlock()
        SessSvc-->>AuthCtrl: Propagate MaxSessionException to GlobalExceptionHandler
        AuthCtrl-->>Client: 409 Conflict (SessionLimitErrorResponse: error, challengeToken, maxSessions)
    end
```

---

### 5.3 Refresh Token Rotation, Grace Cache & Reuse Breach Detection

When an Access Token expires, the client calls `POST /api/v1/auth/refresh-token`. `SessionServiceImpl.validateAndRotate` implements **double-checked locking** with a **2-second Redis grace window** (`quickbite:token-grace:<oldHash>`) to handle concurrent requests from multiple browser tabs or network retries without false-positive breach lockouts.

```mermaid
sequenceDiagram
    autonumber
    participant Client as Client (Web or Mobile)
    participant AuthCtrl as AuthenticationController
    participant SessSvc as SessionServiceImpl
    participant Redis as Redis (Grace Cache & Redisson)
    participant PersistSvc as SessionPersistenceServiceImpl
    participant DB as PostgreSQL

    Client->>AuthCtrl: POST /api/v1/auth/refresh-token (Cookie or JSON RefreshRequest)
    AuthCtrl->>AuthCtrl: Extract rawRefreshToken based on ClientType
    AuthCtrl->>SessSvc: validateAndRotate(rawRefreshToken)
    SessSvc->>SessSvc: hash = TokenUtils.sha256(rawRefreshToken)

    SessSvc->>Redis: 1. Fast Path: getRotatedTokenGrace(hash)
    alt Grace Cache Hit (Concurrent Tab Replay within 2s)
        Redis-->>SessSvc: cachedNewRawToken
        SessSvc->>DB: Resolve sessionId & userId by hash
        SessSvc-->>AuthCtrl: IssuedToken (cachedNewRawToken)
        AuthCtrl-->>Client: 200 OK (New Access Token + Cached Refresh Token)
    else Grace Cache Miss
        SessSvc->>Redis: 2. Acquire Lock ("quickbite:refresh-lock:" + hash, wait 3s, lease 5s)
        SessSvc->>Redis: 3. Double-Check: getRotatedTokenGrace(hash)
        alt Grace Cache Hit After Lock Wait
            Redis-->>SessSvc: cachedNewRawToken
            SessSvc-->>AuthCtrl: IssuedToken (cachedNewRawToken)
            AuthCtrl-->>Client: 200 OK
        else Proceed with Authoritative Rotation
            SessSvc->>DB: findRefreshTokenWithFamilyAndSessionByTokenHash(hash)
            SessSvc->>SessSvc: Verify token, family, session not expired/revoked & user active
            SessSvc->>PersistSvc: markTokenUsed(tokenId, now)
            PersistSvc->>DB: UPDATE refresh_tokens SET used_at = :now WHERE id = :id AND used_at IS NULL

            alt Rows Updated == 0 (BREACH DETECTED: Token Already Used!)
                Note over SessSvc, DB: Compromised token replayed outside 2s grace window!
                SessSvc->>PersistSvc: revokeBreachedFamily(familyId, sessionId, userId) [REQUIRES_NEW]
                PersistSvc->>DB: SET family.revoked_at & reuse_detected_at, SET session.revoked_at (COMMITTED)
                SessSvc-->>AuthCtrl: Throw AuthenticationException ("Refresh token reuse detected...")
                AuthCtrl-->>Client: 401 Unauthorized
            else Rows Updated == 1 (SAFE ROTATION PATH)
                SessSvc->>PersistSvc: saveRotatedToken(family, session, newRawToken, gen + 1, expiresAt)
                PersistSvc->>DB: INSERT RefreshToken (generation = gen + 1) & UPDATE session.last_used_at
                SessSvc->>Redis: cacheRotatedTokenGrace(oldHash, newRawToken, TTL=2s)
                SessSvc-->>AuthCtrl: IssuedToken (newRawToken, sessionId, userId)
                AuthCtrl-->>Client: 200 OK (New Access Token + New Refresh Token via Cookie/Body)
            end
        end
    end
```

---

### 5.4 Max Session Limit Challenge & 3-Step Eviction Flow

When login returns `409 Conflict` (`SessionLimitErrorResponse`), the client uses the returned `challengeToken` (`aud: quickbite-auth` &rarr; `SCOPE_AUTH`) to inspect active sessions, revoke one or more sessions, and claim the new session.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Client
    participant ChallengeCtrl as SessionChallengeController
    participant ChallengeSvc as AuthServiceImpl (SessionChallengeService)
    participant SessSvc as SessionServiceImpl
    participant PersistSvc as SessionPersistenceServiceImpl
    participant DB as PostgreSQL

    Note over User, Client: Prerequisite: Login returned 409 Conflict with challengeToken (5m TTL)

    Client->>ChallengeCtrl: 1. GET /api/v1/auth/sessions (Bearer challengeToken)
    ChallengeCtrl->>ChallengeSvc: getActiveSessionsForUser(userId)
    ChallengeSvc->>DB: findActiveByUserId(userId)
    DB-->>Client: 200 OK List<SessionResponse> (deviceName, deviceOS, ipAddress, daysLeft)

    User->>Client: Selects sessionId to terminate
    Client->>ChallengeCtrl: 2. DELETE /api/v1/auth/sessions/{sessionId} (Bearer challengeToken)
    ChallengeCtrl->>ChallengeSvc: revokeSession(userId, sessionId)
    ChallengeSvc->>PersistSvc: revokeSession(userId, sessionId)
    PersistSvc->>DB: Revoke RefreshTokenFamily & Session (scoped by sessionId AND userId)
    ChallengeCtrl-->>Client: 204 No Content

    Client->>ChallengeCtrl: 3. POST /api/v1/auth/claim-session (Bearer challengeToken + X-Client-Type)
    ChallengeCtrl->>ChallengeSvc: claimSession(userId, deviceInfo)
    ChallengeSvc->>SessSvc: createNewSession(user, deviceInfo)
    SessSvc->>PersistSvc: persistNewSession (verifies activeCount < 3 under Redisson lock)
    PersistSvc->>DB: INSERT new Session, Family, and RefreshToken (gen=1)
    ChallengeSvc-->>ChallengeCtrl: AuthResponse (New Access Token + Refresh Token)
    ChallengeCtrl-->>Client: 200 OK (Set-Cookie for Web Browser OR JSON body for Mobile App)
```

---

### 5.5 Single-Device vs. Global Logout Flow (`LogoutController`)

Protected by `@PreAuthorize("hasAuthority('SCOPE_API')")`:
1. **`POST /api/v1/auth/logout` (Current Device)**:
   - `AuthenticatedSessionResolver.currentSession(jwt)` extracts both `sub` (`userId`) and `session_id` (`sessionId`) from the Access Token.
   - `logoutService.logoutCurrentSession(userId, sessionId)` revokes only that session and its `RefreshTokenFamily`.
   - Returns `204 No Content` with `Set-Cookie: qb_refresh_token=""; Path=/api/v1/auth; Max-Age=0; Secure; HttpOnly; SameSite=Strict`.
2. **`POST /api/v1/auth/logout-all` (All Devices)**:
   - Extracts `userId` from the Access Token.
   - `logoutService.logoutAllSessions(userId)` bulk-revokes all active sessions and token families belonging to the user.
   - Returns `204 No Content` and clears the refresh cookie.

---

## 6. Device Resolution & Background Maintenance

### 6.1 YAUAA Adapter & Caffeine Cache (`YauaaDeviceInfoResolver`)
* **Adapter Pattern**: `YauaaDeviceInfoResolver` implements `DeviceInfoResolver`, isolating the heavy `nl.basjes.parse.useragent.UserAgentAnalyzer` library from the rest of the domain.
* **Selective Field Extraction**: Configures `UserAgentAnalyzer` to extract only `DEVICE_NAME`, `DEVICE_BRAND`, `OPERATING_SYSTEM_NAME`, `OPERATING_SYSTEM_VERSION`, `AGENT_NAME`, and `AGENT_VERSION_MAJOR`.
* **W-TinyLFU Caching**: Wraps parsing in a Caffeine cache (`maximumSize = 10_000`, `expireAfterAccess = 24h`), reducing repeated User-Agent parsing overhead to sub-microsecond map lookups.

### 6.2 Stale Refresh Token Cleanup (`RefreshTokenCleanupScheduler`)
* **Schedule**: Runs daily at `03:00 AM` (`quickbite.auth.cleanup-cron = 0 0 3 * * *`).
* **Retention Window**: Defaults to `30` days (`quickbite.auth.stale-token-retention-days = 30`).
* **Purge Query**:
  ```sql
  DELETE FROM refresh_tokens
  WHERE (revoked_at IS NOT NULL OR used_at IS NOT NULL OR expires_at < :now)
    AND created_at < :cutoff
  ```
* **Rationale**: Retaining rotated (`used_at IS NOT NULL`) tokens for 30 days ensures that if an attacker replays a stolen refresh token days after rotation, `findRefreshTokenWithFamilyAndSessionByTokenHash` still locates the historical token record and triggers breach revocation on the active session.

---

## 7. Configuration Reference (`application.properties`)

```properties
# 1. Session Concurrency & Cleanup
quickbite.auth.max-concurrent-sessions=3
quickbite.auth.stale-token-retention-days=30
quickbite.auth.cleanup-cron=0 0 3 * * *

# 2. Cookie Transport Layer (Web Browser Clients)
quickbite.auth.cookie.refresh-token-name=qb_refresh_token
quickbite.auth.cookie.refresh-token-path=/api/v1/auth

# 3. JWT Crypto & Token Lifetimes
quickbite.auth.jwt.private-key=${JWT_SECRET:classpath:certs/private.pem}
quickbite.auth.jwt.public-key=${JWT_PUBLIC_KEY:classpath:certs/public.pem}
quickbite.auth.jwt.access-token-expiry=PT15M
quickbite.auth.jwt.access-token-audience=quickbite-api
quickbite.auth.jwt.refresh-token-expiry=P7D
quickbite.auth.jwt.challenge-token-expiry=PT5M
quickbite.auth.jwt.challenge-token-audience=quickbite-auth

# 4. CORS Allowed Origins
quickbite.auth.cors.allowed-origins=http://localhost:3000,http://localhost:8080,https://localhost:8080

# 5. Super Admin Bootstrap
quickbite.bootstrap.admin.enabled=${ADMIN_BOOTSTRAP_ENABLED:true}
quickbite.bootstrap.admin.name=${ADMIN_BOOTSTRAP_NAME:Super Admin}
quickbite.bootstrap.admin.email=${ADMIN_BOOTSTRAP_EMAIL:super.admin@quickbite.com}
quickbite.bootstrap.admin.phone-number=${ADMIN_BOOTSTRAP_PHONE:+911234567890}
quickbite.bootstrap.admin.password=${ADMIN_BOOTSTRAP_PASSWORD:Admin@123}
```

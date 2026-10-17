# QuickBite Technical Documentation

Welcome to the QuickBite Backend technical documentation directory. This collection of guides provides in-depth architectural breakdowns, API references, domain specifications, and operational manuals for engineers working on or integrating with the QuickBite platform.

---

## Documentation Index

### Core Architecture & System Design
- [**System Architecture & Domain Design (`architecture.md`)**](architecture.md)
  - Domain-Driven Vertical Slice architecture principles and boundaries.
  - Event-driven cross-domain choreography using Apache Kafka.
  - Core domain state machines (Order, Dispatch, Application Onboarding, Payment).
  - Distributed concurrency control: Redisson locks, PostgreSQL optimistic locking, and Redis caching.

### Security & Authentication
- [**Authentication & Session Management Architecture (`authentication.md`)**](authentication.md)
  - Detailed architectural analysis of `com.quickbite.quickbite.auth`.
  - Self-issued asymmetric RSA JWTs (`aud: quickbite-api` Access Tokens vs. `aud: quickbite-auth` Challenge Tokens).
  - Client-aware hybrid refresh token transport (`X-Client-Type: web_browser` Cookie vs. `mobile_app` JSON body).
  - Generation-tracked refresh token family rotation, 2-second Redis grace window, and reuse breach detection.
  - Daily stale token retention cleanup scheduler (`RefreshTokenCleanupScheduler`).
- [**Client JWT & Session Integration Guide (`jwt.md`)**](jwt.md)
  - Client-facing integration manual for Web Browser (`HttpOnly` cookie) and Mobile App (JSON body) clients.
  - Step-by-step resolution of the `409 Conflict` (Max Sessions Exceeded) 3-step challenge flow.
  - cURL and JavaScript `fetch` examples for login, authenticated requests, session eviction, and token rotation.

### API & Developer References
- [**REST API Catalog & RBAC Reference (`api-guide.md`)**](api-guide.md)
  - Complete index of all 35+ REST controllers across domains.
  - Role-Based Access Control (RBAC) matrix for Public, Customer, Restaurant Owner, Delivery Agent, and Admin roles.
  - Path mappings, request/response structures, and query parameters.

### Domain Deep Dives
- [**Order & Cart Lifecycle Guide (`order-process.md`)**](order-process.md)
  - Single-restaurant shopping cart rules, rolling 24-hour TTL, and real-time checkout preview.
  - Chain-of-Responsibility delivery fee calculation (Base + Distance + Surge) with 5-minute Redis route caching.
  - Non-transactional 2-phase checkout orchestration with `expectedTotal` price-drift protection (`409 Conflict`).
  - Complete 11-state `OrderStatus` machine (`AWAITING_PAYMENT` &rarr; `PLACED` &rarr; `ACCEPTED` &rarr; `PREPARING` &rarr; `READY_FOR_PICKUP` &rarr; `OUT_FOR_DELIVERY` &rarr; `DELIVERED`).
  - Dual-layer timeout architecture: Redisson Delayed Queues (`RDelayedQueue` + atomic `BLPOP` workers) paired with safety-net cron reapers for the 15-minute payment abandonment window and 2-minute restaurant acceptance deadline.
- [**Dispatch Engine & Geospatial Routing (`dispatch-and-routing.md`)**](dispatch-and-routing.md)
  - 3-round expanding radius dispatch algorithm (3km @ 2m &rarr; 6km @ 3m &rarr; 10km @ 5m).
  - 45-second driver offer lifecycle and candidate selection strategies.
  - Redis geospatial tracking (`GEOADD`, `GEORADIUSBYMEMBER`) with PostgreSQL synchronization.
  - 3-tier Composite Routing Gateway (OSRM native routing &rarr; Mapbox API &rarr; Haversine fallback).
  - Dynamic delivery fee and driver earnings calculation engines.
- [**Payments, Webhooks & Automated Reconciliation (`payments-and-reconciliation.md`)**](payments-and-reconciliation.md)
  - Strategy Pattern implementation (`OnlinePaymentStrategy`, `CodPaymentStrategy`, `StubOnlinePaymentStrategy`).
  - Razorpay order creation outside DB transactions, client checkout verification, and HMAC-SHA256 webhooks.
  - Multi-attempt payment reconciliation (`First-Success-Wins`), duplicate capture auto-refunds, and late-payment guards.
  - Kafka event-driven payment refund orchestration.

### Operations & Setup
- [**Installation & Environment Setup (`installation.md`)**](installation.md)
  - Step-by-step local setup for Docker Compose (PostgreSQL 18 + PostGIS, Redis 8, Apache Kafka 4.3).
  - Native OSRM routing engine installation on macOS/Linux.
  - Spring profile configuration (`dev,razorpay` vs. `prod`).
  - Environment variables specification (`.env` vs. `.env.local`).
  - Super admin bootstrap runner credentials and verification steps.
- [**Developer Scratchpad & Notes (`roughbook.md`)**](roughbook.md)
  - Working notes and local commands collected during active development.

---

## Domain Overview Map

```
com.quickbite.quickbite/
├── allotment/       # Workload-balanced admin application distribution
├── auth/            # Dual-JWT tokens, refresh token families & session concurrency
├── cart/            # Single-restaurant shopping carts & checkout pricing preview
├── common/          # S3 storage, composite routing, global exceptions, audit base
├── delivery/        # Multi-round dispatch, driver offers, Redis GEO tracking
├── menu/            # Cuisines catalog/approval & restaurant menu management
├── notification/    # Joined-table notification root, delivery channels & Kafka listeners
├── onboarding/      # Multi-step onboarding for restaurants, drivers & vehicles
├── order/           # Redisson-locked order creation, fee calculation, restaurant deadline
├── payment/         # Razorpay gateway, COD/online strategies, webhooks & reconciliation
├── restaurant/      # PostGIS nearby search, operating hours, document/image galleries
├── review/          # Verified customer reviews & Kafka-driven rating recalculation
├── user/            # Profile management, multi-address book with spatial coordinates
└── vehicle/         # Driver vehicle management, VIN validation, ownership status
```

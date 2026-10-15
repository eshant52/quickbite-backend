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
  - Self-issued asymmetric RSA JWTs (Access Tokens vs. Challenge Tokens).
  - Refresh Token family rotation and breach detection.
  - Stateful session persistence in PostgreSQL with Redisson locking.
- [**Client JWT & Session Integration Guide (`jwt.md`)**](jwt.md)
  - Client-facing integration manual for handling Bearer tokens and HttpOnly cookies.
  - Handling the `409 Conflict` (Max Sessions Exceeded) challenge flow.
  - cURL examples for login, authenticated requests, session eviction, and refresh.

### API & Developer References
- [**REST API Catalog & RBAC Reference (`api-guide.md`)**](api-guide.md)
  - Complete index of all 35+ REST controllers across domains.
  - Role-Based Access Control (RBAC) matrix for Public, Customer, Restaurant Owner, Delivery Agent, and Admin roles.
  - Path mappings, request/response structures, and query parameters.

### Domain Deep Dives
- [**Dispatch Engine & Geospatial Routing (`dispatch-and-routing.md`)**](dispatch-and-routing.md)
  - 3-round expanding radius dispatch algorithm (3km @ 2m → 6km @ 3m → 10km @ 5m).
  - 45-second driver offer lifecycle and candidate selection strategies.
  - Redis geospatial tracking (`GEOADD`, `GEORADIUSBYMEMBER`) with PostgreSQL synchronization.
  - 3-tier Composite Routing Gateway (OSRM native routing → Mapbox API → Haversine fallback).
  - Dynamic delivery fee and driver earnings calculation engines.
- [**Payments, Webhooks & Automated Reconciliation (`payments-and-reconciliation.md`)**](payments-and-reconciliation.md)
  - Strategy Pattern implementation (`RazorpayPaymentStrategy`, `CodPaymentStrategy`, `StubOnlinePaymentStrategy`).
  - Razorpay order creation, client checkout, and HMAC-SHA256 webhook verification.
  - Automated order abandonment scheduler (15-min TTL) and payment reconciliation background jobs.
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

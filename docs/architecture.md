# QuickBite System Architecture & Domain Design

This document provides a comprehensive architectural breakdown of the QuickBite backend platform, detailing its Domain-Driven Vertical Slice architecture, event-driven messaging topology, core state machines, and concurrency control models.

---

## 1. System Overview & Technology Stack

QuickBite is an enterprise-grade food ordering, restaurant management, and hyper-local delivery platform built with a modular, domain-driven vertical slice structure.

```mermaid
flowchart TB
    subgraph Clients["Clients"]
        CustomerClient["Customer Web / Mobile"]
        OwnerClient["Restaurant Web Portal"]
        DriverClient["Delivery Agent Mobile"]
        AdminClient["Admin Web Console"]
    end

    subgraph SecurityPerimeter["Security Perimeter"]
        SecurityFilter["Spring Security Filter Chain"]
        NimbusJwt["Nimbus RSA JWT Decoder"]
        AuthConverter["JwtAuthenticationConverter<br/>(SCOPE_API + ROLE_* vs SCOPE_AUTH)"]
    end

    subgraph ApplicationLayer["Application Core (14 Bounded Contexts)"]
        direction TB
        AuthDomain["auth: Dual-Token & Sessions"]
        UserDomain["user: Profiles & Addresses"]
        OnboardingDomain["onboarding: 3-Way Pipeline"]
        AllotmentDomain["allotment: Workload Balancer"]
        RestaurantDomain["restaurant: Spatial Catalog"]
        MenuDomain["menu: Cuisines & Items"]
        CartDomain["cart: Cart Validation"]
        OrderDomain["order: Order & Pricing Engine"]
        PaymentDomain["payment: Gateway & Reconcile"]
        DeliveryDomain["delivery: Multi-Round Dispatch"]
        VehicleDomain["vehicle: Driver Fleet"]
        ReviewDomain["review: Ratings & Feedback"]
        NotificationDomain["notification: Joined Events"]
        CommonDomain["common: S3, Routing, Base"]
    end

    subgraph StorageLayer["Persistence & Caching"]
        PostgresDB[(PostgreSQL 18 + PostGIS<br/>Audited by Hibernate Envers)]
        RedisCache[(Redis 8<br/>Cache, Sessions, Redis GEO)]
        RedissonLock[Redisson Distributed Locks]
    end

    subgraph EventStreaming["Event Streaming"]
        KafkaBroker{{Apache Kafka 4.3<br/>7 Domain Streams + DLTs}}
    end

    subgraph ExternalServices["External Providers"]
        OSRMServer[OSRM Routing Engine :5001]
        AWSS3[AWS S3 Object Storage]
        RazorpayGateway[Razorpay Payment API]
    end

    Clients --> SecurityPerimeter
    SecurityPerimeter --> ApplicationLayer
    ApplicationLayer <--> PostgresDB
    ApplicationLayer <--> RedisCache
    ApplicationLayer <--> RedissonLock
    ApplicationLayer <--> KafkaBroker
    ApplicationLayer <--> OSRMServer
    ApplicationLayer <--> AWSS3
    ApplicationLayer <--> RazorpayGateway
```

### Core Technology Stack

| Component | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Runtime & Language** | Java | 25 | Latest enterprise JVM with virtual threads & pattern matching |
| **Framework** | Spring Boot | 4.1.0 | Core microservice framework (Spring Data, WebMVC, Security) |
| **Primary Database** | PostgreSQL + PostGIS | 18.1 / 3.4 | Authoritative relational storage with native geospatial capabilities |
| **Auditing** | Hibernate Envers | Bundled | Historical audit trails on key aggregates (`User`, `Restaurant`, `Order`, `Payment`, `DeliveryAgent`) |
| **Cache & In-Memory** | Redis | 8.4-alpine | Session TTLs, token rotation grace windows, and real-time geospatial driver tracking |
| **Distributed Locks** | Redisson | 4.7.0 | Distributed concurrency locks (`RLock`) for critical transactions |
| **Event Broker** | Apache Kafka | 4.3.0 | Asynchronous domain event streaming with Dead Letter Queues (`.DLT`) |
| **File Storage** | AWS S3 SDK | 2.31.0 | S3 presigned PUT URLs for direct client document/image uploads |
| **Routing Engine** | OSRM / Mapbox | Native / Cloud | Multi-tier routing gateway for street-level travel distances and driving duration |
| **Payment Gateway** | Razorpay SDK | 1.4.10 | Multi-method payments (UPI, Cards, NetBanking, COD) and webhook processing |

---

## 2. Domain-Driven Vertical Slice Architecture

QuickBite is strictly structured as **independent bounded contexts** located within `com.quickbite.quickbite.<domain>`.

### Package Architecture Rules
1. **Vertical Isolation**: Each domain owns its `controller/`, `service/`, `dto/`, `model/`, `repository/`, and `exception/` packages.
2. **Zero Entity Leakage**: Entities are private to their domain. Controllers never serialize JPA entities directly; all requests and responses use immutable Java `record` DTOs.
3. **Cross-Domain Communication**:
   - Asynchronous communication is preferred via **Kafka domain events**.
   - Synchronous interactions between domains are mediated through dedicated integration services or shared DTOs defined in `common/`.
   - Domains **never** import another domain's internal JPA repositories or mutate another domain's entities directly.
4. **Data Inheritance**: All database entities extend `common.model.Base`, which provides time-ordered **UUIDv7** primary keys, `createdAt`, and `updatedAt` audit timestamps.

```
com.quickbite.quickbite.<domain>/
├── controller/          # REST controllers — thin, HTTP status codes, delegation
├── dto/                 # Request & Response records (immutable data contracts)
├── exception/           # Domain-specific runtime exceptions
├── model/               # JPA entities, Hibernate spatial types, and PostgreSQL enums
├── repository/          # Spring Data JPA repositories
├── service/             # Business logic interfaces and implementations
│   └── strategy/        # Strategy pattern implementations (e.g., assignment, payment)
└── util/                # Domain-specific helpers and mappers
```

---

## 3. Event-Driven Architecture with Apache Kafka

Asynchronous decoupling across bounded contexts is implemented using **Apache Kafka**. Domain publishers emit strongly-typed event DTOs, and independent listener services consume events with idempotent processing and retry mechanisms.

### Kafka Topic Topology & Consumer Mapping

The system declares **7 primary aggregate streams** alongside automated Dead Letter Topics (`.DLT`):

```mermaid
flowchart LR
    subgraph Publishers["Domain Publishers"]
        OrderPub[OrderKafkaEventPublisher]
        PaymentPub[PaymentKafkaEventPublisher]
        DeliveryPub[DeliveryKafkaEventPublisher]
        RestAppPub[RestaurantAppKafkaPublisher]
        AgentAppPub[DeliveryAgentAppKafkaPublisher]
        VehicleAppPub[VehicleAppKafkaPublisher]
        CuisinePub[CuisineKafkaEventPublisher]
    end

    subgraph Topics["Kafka Topics (3 Partitions, 1-3 Replicas)"]
        T1["quickbite.order.events<br/>(+ .DLT)"]
        T2["quickbite.payment.events<br/>(+ .DLT)"]
        T3["quickbite.delivery.events<br/>(+ .DLT)"]
        T4["quickbite.restaurant.application.events<br/>(+ .DLT)"]
        T5["quickbite.deliveryagent.application.events<br/>(+ .DLT)"]
        T6["quickbite.vehicle.application.events<br/>(+ .DLT)"]
        T7["quickbite.cuisine.events<br/>(+ .DLT)"]
    end

    subgraph Consumers["Domain Listeners"]
        OrderListener[order.PaymentEventListener]
        DeliveryListener[delivery.OrderAcceptedKafkaListener]
        CartListener[cart.CartEventListener]
        ReviewListener[review.RestaurantRatingEventListener]
        PaymentRefundListener[payment.PaymentRefundListener]
        NotificationListeners[notification.*EventListener<br/>(Consumes all 7 streams)]
    end

    OrderPub --> T1
    PaymentPub --> T2
    DeliveryPub --> T3
    RestAppPub --> T4
    AgentAppPub --> T5
    VehicleAppPub --> T6
    CuisinePub --> T7

    T1 --> DeliveryListener
    T1 --> CartListener
    T1 --> NotificationListeners
    T2 --> OrderListener
    T2 --> NotificationListeners
    T3 --> NotificationListeners
    T4 --> NotificationListeners
    T5 --> NotificationListeners
    T6 --> NotificationListeners
    T7 --> NotificationListeners
```

### Poison-Pill Resilience & Dead Letter Strategy
- **Error Handler**: Configured via `DefaultErrorHandler` with `FixedBackOff(1_000L, 3)`.
- **Dead Letter Recovery**: Messages failing after 3 consecutive retry attempts are automatically rerouted to `<topic>.DLT` via Spring Kafka's `DeadLetterPublishingRecoverer`.
- **Polymorphic Serialization**: `RecordMessageConverter` uses `StringJacksonJsonMessageConverter` with Jackson `INFERRED` type mapping so consumers receive typed Java DTOs directly without boilerplate parsing.

---

## 4. Core Domain State Machines

### 4.1 Order State Machine (`OrderStatus`)

The complete order lifecycle encompasses customer placement, payment verification, restaurant acceptance, food preparation, multi-round delivery dispatch, and delivery confirmation.

```mermaid
stateDiagram-v2
    [*] --> PLACED: Customer places order (Redisson locked)
    PLACED --> PAYMENT_PENDING: Online payment initiated
    PLACED --> CONFIRMED: Cash on Delivery (COD) selected
    
    PAYMENT_PENDING --> PAID: Payment verified / Webhook success
    PAYMENT_PENDING --> PAYMENT_FAILED: Payment fails / Webhook failure
    PAYMENT_PENDING --> ABANDONED: OrderAbandonmentScheduler (15 min TTL)
    
    PAYMENT_FAILED --> PAYMENT_PENDING: Customer retries payment
    PAYMENT_FAILED --> CANCELLED: Payment expired / aborted
    
    PAID --> CONFIRMED: Transition to confirmed state
    
    CONFIRMED --> PREPARING: Restaurant accepts order (Within 2m deadline)
    CONFIRMED --> CANCELLED: Restaurant declines OR 2m acceptance deadline expires (Auto-refund triggered)
    
    PREPARING --> READY_FOR_PICKUP: Kitchen marks food ready
    READY_FOR_PICKUP --> OUT_FOR_DELIVERY: Delivery agent picks up order
    
    OUT_FOR_DELIVERY --> DELIVERED: Agent completes delivery with customer
    
    CANCELLED --> REFUNDED: Refund processed via Kafka
    CANCELLED --> REFUND_FAILED: Gateway refund failure
    DELIVERED --> [*]
    REFUNDED --> [*]
    ABANDONED --> [*]
```

### 4.2 Delivery Dispatch & Offer State Machine (`DeliveryDispatchStatus`, `DeliveryOfferStatus`)

Orders requiring delivery trigger the automated multi-round dispatch engine upon restaurant acceptance.

```mermaid
stateDiagram-v2
    [*] --> PENDING: Order confirmed / preparing
    PENDING --> ROUND_1: Radius: 3km, Duration: 2min
    
    ROUND_1 --> OFFERED: Offer sent to candidate drivers (45s timeout)
    OFFERED --> ACCEPTED: Driver accepts offer
    OFFERED --> REJECTED: Driver declines offer
    OFFERED --> EXPIRED: 45s timer expires without action
    
    REJECTED --> ROUND_1: Check next driver in current round pool
    EXPIRED --> ROUND_1: Check next driver in current round pool
    
    ROUND_1 --> ROUND_2: Round 1 pool exhausted (Radius: 6km, Duration: 3min)
    ROUND_2 --> ROUND_3: Round 2 pool exhausted (Radius: 10km, Duration: 5min)
    
    ROUND_3 --> EXHAUSTED: No drivers found in 10km after 10 minutes
    
    ACCEPTED --> ASSIGNED: Driver assigned to order
    ASSIGNED --> COMPLETED: Delivery completed
    EXHAUSTED --> [*]
    COMPLETED --> [*]
```

### 4.3 Onboarding Application State Machine (`ApplicationStatus`)

Applies to Restaurant Applications, Delivery Agent Applications, and Vehicle Applications.

```mermaid
stateDiagram-v2
    [*] --> DRAFT: Partner creates application
    DRAFT --> SUBMITTED: Required documents & details provided
    
    SUBMITTED --> UNDER_REVIEW: AdminAllotment assigns application to Admin
    UNDER_REVIEW --> APPROVED: Admin approves (Entities created in core tables)
    UNDER_REVIEW --> REJECTED: Admin rejects with reason
    
    REJECTED --> REOPENED: Partner updates documents & resubmits
    REOPENED --> UNDER_REVIEW: Resubmitted for admin review
    
    APPROVED --> [*]
```

### 4.4 Payment State Machine (`PaymentStatus`)

```mermaid
stateDiagram-v2
    [*] --> PENDING: Payment record created
    PENDING --> SUCCESS: Razorpay signature verified / Webhook success
    PENDING --> FAILED: Payment failure received
    PENDING --> CANCELLED: Order cancelled before payment
    
    SUCCESS --> REFUND_PENDING: Order cancellation or rejection
    REFUND_PENDING --> REFUNDED: Gateway acknowledges refund
    REFUND_PENDING --> REFUND_FAILED: Gateway refund rejection
    
    FAILED --> [*]
    CANCELLED --> [*]
    REFUNDED --> [*]
    REFUND_FAILED --> [*]
```

---

## 5. Distributed Concurrency, Locking & Data Consistency

### 5.1 Redisson Distributed Locking (`RLock`)
Distributed locks are applied across nodes using Redisson to prevent race conditions during high-concurrency operations:
- **Order Placement**: Locked per user (`order:user:{userId}`) to prevent double-clicks and concurrent cart checkout attempts within a 5-second cooldown window.
- **Payment Capture & Webhooks**: Locked per order (`payment:order:{orderId}`) to ensure client-side verification and background webhook processing do not execute parallel state transitions.
- **Delivery Offer Acceptance**: Locked per dispatch (`dispatch:order:{orderId}`) to ensure only the first accepting driver wins the offer; subsequent race attempts fail gracefully.
- **Session Concurrency Management**: Locked per user during authentication and session challenge eviction.

### 5.2 PostgreSQL Optimistic Locking (`@Version`)
Aggregates subject to concurrent modifications (e.g., `Order`, `Payment`, `DeliveryAgent`, `DeliveryOffer`, `Cart`) feature integer `@Version` columns. If an outdated process attempts a write, Hibernate throws an `OptimisticLockException`, protecting authoritative data from lost updates.

### 5.3 Authoritative Session Enforcement & Redis Grace Cache
- Active user sessions and concurrent session limits (`max-concurrent-sessions = 3`) are authoritatively stored and validated against **PostgreSQL**.
- When a refresh token is rotated, the previous token hash is stored in **Redis for 2 seconds**. If duplicate concurrent requests arrive from the same browser tab, they receive the cached new token instead of erroneously triggering family breach invalidation.

---

## 6. Geospatial & Routing Architecture

### 6.1 PostGIS Spatial Storage
- Geolocation coordinates are stored using JTS (Java Topology Suite) `Point` objects mapped to SRID **4326** (WGS 84).
- Column definition: `GEOMETRY(POINT, 4326)`.
- Spatial indices are created using PostgreSQL `GIST` indexes for fast bounding box and radius queries (`ST_DWithin`, `ST_DistanceSphere`).

### 6.2 Redis GEO Real-Time Tracking
- Driver GPS pings are updated in Redis using `GEOADD quickbite:delivery:agent:locations <longitude> <latitude> <agentId>`.
- Nearby candidate drivers within dispatch radius are queried in sub-millisecond time using `GEORADIUSBYMEMBER`.
- Driver location is backed by a 60-second Redis TTL heartbeat.
- A background scheduler (`DeliveryLocationSyncScheduler`) synchronizes active driver locations from Redis into PostgreSQL every 30 seconds for audit and tracking persistence.

### 6.3 3-Tier Composite Routing Gateway
All distance and travel duration calculations flow through a composite fallback chain:
1. **Primary: OSRM Routing Adapter** — Local high-performance routing machine querying native street graphs on port `5001`.
2. **Secondary: Mapbox Routing Adapter** — Cloud API fallback used when OSRM is offline or disabled.
3. **Tertiary: Haversine Fallback Adapter** — Pure mathematical great-circle distance calculator with estimated urban driving speeds (25 km/h) ensuring zero system downtime.

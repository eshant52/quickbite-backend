# QuickBite Agent Guide

## Project Snapshot
- **Framework**: Spring Boot 4.1.0 on Java 25 (`pom.xml`).
- **Entry Point**: `QuickBiteApplication.java` — enables JPA auditing, caching, and `@ConfigurationPropertiesScan`.
- **Architecture**: Domain-driven vertical slice. Each bounded context (`auth`, `user`, `restaurant`, `order`, etc.) owns its own `controller/`, `service/`, `dto/`, `model/`, `repository/`, and `exception/` packages.
- **State**: Auth, User, and Onboarding modules have controllers/services/repositories implemented. Other domains (`order`, `payment`, `delivery`, `cart`, `menu`, `review`, `vehicle`, `notification`) are currently **model-only** — entities and enums exist but no controllers, services, or repositories yet.

---

## Package Layout & Where to Put Things

Every new feature must live inside its own domain package under `com.quickbite.quickbite.<domain>`:

```
com.quickbite.quickbite.<domain>/
├── controller/          # REST controllers — thin, delegates to service layer
├── dto/                 # Request/Response records — never expose entities directly
├── exception/           # Domain-specific exceptions (e.g., OrderNotFoundException)
├── model/               # JPA entities and enums
├── repository/          # Spring Data JPA interfaces
├── service/             # Business logic interfaces and implementations
│   └── strategy/        # Strategy pattern implementations (see §Design Patterns)
└── util/                # Domain-specific utilities (parsers, converters)
```

### Shared / Cross-Cutting Code
```
com.quickbite.quickbite.common/
├── config/              # @Configuration beans (Security, JWT, Redis, Kafka)
├── event/               # Kafka topic constants and event DTOs
├── exception/           # Global exceptions and @RestControllerAdvice handler
├── model/               # Shared superclasses (Base.java) and shared enums
├── factory/             # Abstract factories for cross-domain object creation
└── util/                # Shared utility classes (date formatters, validators)
```

### Rules
- **Never** import from one domain into another domain's internal classes. If two domains need to communicate, use events (Kafka), shared DTOs in `common/`, or a dedicated integration service.
- **Never** put business logic inside entities. Entities are data holders; services are logic holders.
- **Never** return JPA entities from controllers. Always map to a DTO record.

---

## Domain & Persistence Conventions
- All persistent entities extend `common.model.Base`, which provides UUIDv7 IDs plus `createdAt` / `updatedAt` auditing fields.
- Use Lombok `@Getter` / `@Setter` on entities; fields are package-private via Lombok rather than explicit accessors.
- Bidirectional relationships: `@ManyToOne` / `@OneToOne` on the owning side, `@OneToMany(mappedBy = "...")` on the collection side.
- Enum columns use `@Enumerated(EnumType.STRING)` plus `@JdbcType(PostgreSQLEnumJdbcType.class)` for PostgreSQL native enums. Keep enum names stable and update Flyway migration when adding new values.
- `Notification` is a joined inheritance root (`@Inheritance(strategy = JOINED)`), with subtype tables `OrderNotification` and `PaymentNotification`.
- Spatial data uses JTS `Point` types with `@JdbcTypeCode(SqlTypes.GEOMETRY)` and `columnDefinition = "GEOMETRY(POINT, 4326)"`.
- Hibernate Envers `@Audited` is applied to key aggregates (`User`, `Restaurant`, `Order`, `DeliveryAgent`, `Payment`, `MenuItem`). Non-audited relations must use `@NotAudited` or `@Audited(targetAuditMode = NOT_AUDITED)`.

---

## Schema & Migrations
- Hibernate DDL is set to `validate` — schema changes **must** be expressed as Flyway migrations.
- Flyway is enabled at `classpath:db/migration` with `baseline-on-migrate=true`.
- Existing migrations: `V1` through `V7`. New migrations must follow the naming convention: `V<next>__<snake_case_description>.sql`.
- When adding or changing entities, keep the Java model and Flyway schema strictly aligned.

---

## Security Architecture
- **Dual JWT Tokens**: Access Tokens (`aud: quickbite-api`, 15min) carry `SCOPE_API` + `ROLE_*`. Challenge Tokens (`aud: quickbite-auth`, 5min) carry `SCOPE_AUTH` only.
- **Refresh Tokens**: Opaque SHA-256 hashed strings stored in PostgreSQL with family-based rotation and reuse breach detection. Transported via `HttpOnly`, `Secure`, `SameSite=Strict` cookies.
- **Spring Security**: Stateless sessions, RSA-signed JWTs via Nimbus, audience-based authority mapping in `JwtAuthenticationConverter`.
- **Defense in Depth**: URL path matchers in `SecurityFilterChain` as outer perimeter + `@PreAuthorize` on controller/service methods as inner guard.
- **Password Hashing**: `Argon2PasswordEncoder` (OWASP gold standard).
- **Actuator**: `/actuator/health` and `/actuator/info` are public. All other actuator endpoints require `ROLE_ADMIN`.

---

## Runtime Dependencies
- **PostgreSQL**: `jdbc:postgresql://localhost:5430/quickbite` (env-configurable).
- **Redis**: `localhost:6377` — used for Spring cache, session tracking, and distributed locks.
- **Kafka**: `localhost:9092` — used for domain event messaging.
- **JWT Keys**: RSA key pair at `classpath:certs/private.pem` and `classpath:certs/public.pem`.
- **Multipart Uploads**: Max file 5MB, max request 10MB.

---

## Design Patterns — Where & When to Apply

### Strategy Pattern → `<domain>/service/strategy/`
Use when a domain has **multiple interchangeable algorithms** for the same operation.

**Where to apply in QuickBite:**
- `payment/service/strategy/` — Payment processing differs by provider (Razorpay, Stripe, UPI, COD). Create `PaymentStrategy` interface with implementations like `RazorpayPaymentStrategy`, `UpiPaymentStrategy`, `CodPaymentStrategy`.
- `notification/service/strategy/` — Notification delivery channel varies (SMS, Push, Email, In-App). Create `NotificationDeliveryStrategy` with `SmsNotificationStrategy`, `PushNotificationStrategy`, etc.
- `delivery/service/strategy/` — Delivery agent assignment can use different algorithms (nearest, least-loaded, round-robin). Create `DeliveryAssignmentStrategy`.
- `order/service/strategy/` — Pricing/discount calculation can vary by promo type. Create `PricingStrategy`.

```java
// Example: payment/service/strategy/PaymentStrategy.java
public interface PaymentStrategy {
    PaymentResult process(PaymentRequest request);
    boolean supports(PaymentMethod method);
}

// payment/service/strategy/RazorpayPaymentStrategy.java
@Component
public class RazorpayPaymentStrategy implements PaymentStrategy { ... }
```

### Factory Pattern → `<domain>/service/` or `common/factory/`
Use when object creation logic is complex or varies by type.

**Where to apply in QuickBite:**
- `notification/service/NotificationFactory.java` — Create the correct `Notification` subclass (`OrderNotification`, `PaymentNotification`) based on event type. Centralizes joined-inheritance instantiation.
- `order/service/OrderFactory.java` — Build `Order` aggregate with items, status history, and delivery location from a `PlaceOrderRequest` DTO.

```java
// notification/service/NotificationFactory.java
@Component
public class NotificationFactory {
    public Notification create(NotificationEvent event) {
        return switch (event.type()) {
            case ORDER  -> new OrderNotification(event);
            case PAYMENT -> new PaymentNotification(event);
        };
    }
}
```

### Builder Pattern → DTOs and complex aggregates
Use when constructing objects with many optional parameters.

**Where to apply in QuickBite:**
- Already in use implicitly via Java `record` constructors.
- For complex query result objects, use Lombok `@Builder` on response DTOs:
  - `order/dto/OrderDetailResponse.java` — An order response with nested items, status history, delivery info, and payment details benefits from `@Builder`.

### Adapter Pattern → `<domain>/service/` (external integrations)
Use when wrapping external third-party APIs behind a clean internal interface.

**Where to apply in QuickBite:**
- `auth/service/YauaaDeviceInfoResolver.java` — **Already implemented!** Adapts the Yauaa library behind a `DeviceInfoResolver` interface.
- `payment/service/RazorpayAdapter.java` — Wrap Razorpay SDK calls behind an internal `PaymentGateway` interface.
- `notification/service/FirebaseAdapter.java` — Wrap Firebase Cloud Messaging behind a `PushNotificationSender` interface.
- `delivery/service/GoogleMapsAdapter.java` — Wrap Google Maps Distance Matrix API behind a `DistanceCalculator` interface.

```java
// Adapter interface lives in domain service package
public interface PaymentGateway {
    PaymentResult charge(Money amount, PaymentCredentials credentials);
    RefundResult refund(String transactionId, Money amount);
}

// Adapter implementation wraps the external SDK
@Component
public class RazorpayAdapter implements PaymentGateway { ... }
```

### Observer / Event-Driven Pattern → Kafka events
Use for cross-domain communication without coupling.

**Where to apply in QuickBite:**
- `order` publishes `OrderPlacedEvent` → `notification` listens and creates notifications.
- `order` publishes `OrderStatusChangedEvent` → `delivery` listens and updates agent assignment.
- `payment` publishes `PaymentCompletedEvent` → `order` listens and updates order status.
- Event DTOs go in `common/event/`. Kafka topic constants are in `common/event/QuickBiteTopics.java`.

### Template Method Pattern → `common/service/`
Use when multiple services share the same algorithmic skeleton but differ in specific steps.

**Where to apply in QuickBite:**
- `onboarding/service/AbstractOnboardingProcessor.java` — Both restaurant and delivery partner onboarding follow the same flow (validate → persist → notify admin → await approval), but the validation and entity creation steps differ.

### Decorator Pattern → Cross-cutting service wrappers
Use for transparently adding behavior (logging, caching, metrics) to existing services.

**Where to apply in QuickBite:**
- Spring's `@Cacheable`, `@Transactional`, and `@PreAuthorize` are decorators already in use.
- For custom needs, apply explicitly: e.g., a `LoggingOrderService` wrapping `OrderServiceImpl` to add structured audit logging.

---

## SOLID Principles — Enforcement Guide

### S — Single Responsibility
- **Controllers**: Only handle HTTP concerns (request parsing, response building, status codes). No business logic.
- **Services**: Only handle business rules. No HTTP concerns, no direct entity serialization.
- **Repositories**: Only handle data access. No business rules.
- ❌ **Anti-pattern to avoid**: A controller that queries the database, applies business rules, and builds the response all in one method.

### O — Open/Closed
- Use **Strategy pattern** (see above) so new payment methods or notification channels can be added without modifying existing service classes.
- Use **Spring profiles** (`@Profile("dev")`, `@Profile("prod")`) so environment-specific behavior is added via new beans, not `if-else` branches.
- ❌ **Anti-pattern to avoid**: A giant `switch` statement inside a service that grows every time a new enum value is added.

### L — Liskov Substitution
- All `Notification` subclasses (`OrderNotification`, `PaymentNotification`) must be fully substitutable for the `Notification` base class.
- All `Strategy` implementations must honor the interface contract — no implementation should throw `UnsupportedOperationException`.
- ❌ **Anti-pattern to avoid**: A subclass that overrides a method to do nothing or throw an exception.

### I — Interface Segregation
- **Already followed well**: `AuthService`, `SessionService`, `SessionStoreService`, `AuthCookieService`, `TokenService` are all narrow, focused interfaces.
- When adding new features, prefer small interfaces (`OrderQueryService`, `OrderCommandService`) over a single fat `OrderService` with 20+ methods.
- ❌ **Anti-pattern to avoid**: A single `OrderService` interface that forces implementations to provide methods for placement, tracking, history, analytics, and admin operations.

### D — Dependency Inversion
- **Already followed**: Controllers depend on service interfaces, not implementations. `SessionStoreService` (interface) is implemented by `SessionRedisStoreService`.
- Always inject interfaces, never concrete classes. Spring will wire the implementation automatically.
- For external integrations, always create an interface (e.g., `PaymentGateway`) and code against that. The adapter (e.g., `RazorpayAdapter`) is the implementation detail.
- ❌ **Anti-pattern to avoid**: A service class that directly instantiates another service with `new` instead of constructor injection.

---

## Developer Workflow
- Run `./mvnw test` for verification.
- Run `./mvnw spring-boot:run` for local startup.
- Use `./mvnw clean package` for a packaged artifact.
- If startup fails, check local availability of PostgreSQL, Redis, Kafka, and JWT key files before changing code.

---

## Key Files to Inspect First
- `pom.xml` — Dependencies and Java version.
- `src/main/resources/application.properties` — External service wiring and auth configuration.
- `common/model/Base.java` — Entity inheritance pattern.
- `common/config/SecurityConfig.java` — Security filter chain and token audience mapping.
- `common/config/JwtConfig.java` — RSA key loading and JWT encoder/decoder beans.
- `common/config/AuthProperties.java` — Typed configuration properties record.
- `auth/service/AuthServiceImpl.java` — Authentication orchestration logic.
- `auth/service/SessionServiceImpl.java` — Session lifecycle and token rotation engine.
- `restaurant/model/Restaurant.java` — Complex entity relationship patterns.
- `order/model/Order.java` — Cross-aggregate relationships and spatial data.

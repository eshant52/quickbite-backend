# QuickBite agent guide

## Project snapshot
- Spring Boot 4.1 backend on Java 25 (`pom.xml`).
- Main entry point is `src/main/java/com/quickbite/quickbite/QuickBiteApplication.java`; it enables JPA auditing.
- The codebase is currently model-first: `controllers/` and `services/` are empty, so the domain layer in `models/` defines most of the shape.

## Domain and persistence conventions
- All persistent entities extend `models/Base.java`, which provides UUIDv7 ids plus `createdAt` / `updatedAt` auditing fields.
- Use Lombok `@Getter` / `@Setter` on entities; fields are typically package-private via Lombok rather than explicit accessors.
- Bidirectional relationships follow the pattern `@ManyToOne` / `@OneToOne` on the owning side and `@OneToMany(mappedBy = "...")` on the collection side.
- Example anchors: `Restaurant.menuItems`, `Order.items`, `Cart.items`, and `Payment.statusHistory`.
- Enum columns are usually stored as PostgreSQL enums with `@Enumerated(EnumType.STRING)` plus `@JdbcType(PostgreSQLEnumJdbcType.class)`; keep enum names stable and update schema accordingly.
- `Notification` is a joined inheritance root (`@Inheritance(strategy = JOINED)`), with subtype tables such as `OrderNotification` and `PaymentNotification`.
- Spatial data uses JTS `Point` types for fields like `Address.location`, `Order.deliveryLocation`, and `DeliveryAgent.lastLocation`.

## Schema and migrations
- Hibernate DDL is set to `validate` in `src/main/resources/application.properties`, so schema changes must be expressed in Flyway rather than relying on auto-creation.
- Flyway is enabled and configured for `classpath:db/migration`, but the migration directory is currently empty.
- When adding or changing entities, keep the Java model and Flyway schema aligned.

## Runtime dependencies
- PostgreSQL defaults to `jdbc:postgresql://localhost:5432/quickbite`.
- Redis defaults to `localhost:6379` and is used for Spring cache.
- Kafka defaults to `localhost:9092`.
- JWT resource-server keys are expected at `classpath:certs/private.pem` and `classpath:certs/public.pem`.
- Actuator exposes `health`, `info`, and `metrics` only; multipart uploads are capped at 5 MB / 10 MB.

## Working in this repo
- Keep new code under `com.quickbite.quickbite` so component scanning continues to work.
- Add new behavior by introducing repositories/services/controllers alongside the existing model package rather than putting logic into entities.
- Preserve the current naming style for domain concepts: `RestaurantVerificationStatusHistory`, `VehicleOwnershipDocument`, `OrderStatusHistory`, etc.

## Developer workflow
- Run `./mvnw test` for verification; the only existing test is the Spring context smoke test in `src/test/java/com/quickbite/quickbite/QuickBiteApplicationTests.java`.
- Run `./mvnw spring-boot:run` for local startup.
- Use `./mvnw clean package` when you need a packaged artifact.
- If startup fails, check local availability of PostgreSQL, Redis, Kafka, and the JWT key files before changing code.

## Key files to inspect first
- `pom.xml` for dependencies and Java version.
- `src/main/resources/application.properties` for external service wiring.
- `src/main/java/com/quickbite/quickbite/models/Base.java` and `Restaurant.java` for entity patterns.
- `src/main/java/com/quickbite/quickbite/models/Order.java` and `Notification.java` for cross-aggregate relationships.


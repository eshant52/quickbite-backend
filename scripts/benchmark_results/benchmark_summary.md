# QuickBite Local Benchmark Results

## Test Environment
- **Date / Time:** 20 November 2026 17:34:33 IST
- **Machine:** Apple M2 Pro (arm64), 16.0 GB RAM, macOS 27.2.1
- **Java:** `openjdk version "25.0.3" 2026-04-21 LTS`
- **PostgreSQL / PostGIS:** `PostgreSQL 18.1 (Debian 18.1-1.pgdg13+2)` / PostGIS `3.6 USE_GEOS=1 USE_PROJ=1 USE_STATS=1`
- **Redis:** `verror: NOPERM User quickbite has no permissions to run the 'info' command`
- **Local OSRM:** `reachable (HTTP 200)` (`http://localhost:5001`)
- **Sample Sizes:** 30 timed HTTP iterations (+warmup); 100,000 synthetic orders & 2,000 synthetic agents in rolled-back PostgreSQL transactions.

## Table 5.04 Ready Summary

| Benchmark Scenario / Metric | Baseline (Pre-Optimization / Cold) | Optimized (Post-Optimization / Warm) | Technique Applied |
| :--- | :--- | :--- | :--- |
| 1. Checkout preview route resolution (`GET /cart/checkout-preview`) | p50 7.62 ms (p95 18.80 ms) | p50 5.46 ms (p95 5.96 ms) | 5-minute Redis route cache (`quickbite:route:*`) in `OrderPricingServiceImpl` |
| 2. Customer order history cursor query (100,000 synthetic `orders`) | OFFSET: 4.35 ms via `idx_orders_customer_id, pk_restaurants`; SeqScan: 3.56 ms (Seq Scan) | 0.01 ms via `idx_orders_customer_id, pk_restaurants` | Composite B-tree `idx_orders_customer_id` + split derived cursor queries |
| 3. Restaurant live kitchen queue (100,000 synthetic `orders`) | 7.12 ms (Seq Scan) | 0.01 ms via `idx_orders_restaurant_status, pk_restaurants` | Composite B-tree `idx_orders_restaurant_status` (V33) |
| 4. Order detail API (`GET /customer/orders/{id}`) | 5 queries (0.03 ms DB exec) | 2 queries (0.02 ms DB exec); API p50 4.95 ms (p95 5.76 ms) | `@EntityGraph` eager join + `idx_order_items_order_id` + `idx_order_status_history_order_created` |
| 5. Nearby driver candidate search (5 km radius, 2,000 agents) | 2.43 ms (Seq Scan) | Redis GEO p50 0.85 ms (p95 0.92 ms); PostGIS 2.25 ms via `idx_delivery_agents_dispatch_available` | Redis `GEOSEARCH` + heartbeat `MGET`; PostGIS functional geography GiST `idx_delivery_agents_dispatch_available` |
| 6. Restaurant menu & profile reads (`GET /restaurants/{id}...`) | Menu p50 5.47 ms (p95 6.58 ms); Profile p50 4.86 ms (p95 5.56 ms) | Menu p50 2.33 ms (p95 2.77 ms); Profile p50 2.10 ms (p95 2.34 ms) | Stampede-safe `@Cacheable(sync = true)` in Redis |
| 7. Concurrent checkout pool bound (Analytical Little's Law, 500 ms gateway vs. 10 ms TX) | ~20.0 req/s (pool=10) / ~40.0 req/s (pool=20) | ~1000.0 req/s (pool=10) / ~2000.0 req/s (pool=20) | Non-`@Transactional` `OrderServiceImpl` orchestrator + TX 1 / TX 2 split |
| 8. Idle timeout database polling load | 132 polls/hour | 3 safety-net polls/hour (97.7% reduction) | Redisson `RDelayedQueue` + atomic `BLPOP` consumer + 30m/60m reapers |

## Suggested `Benchmark method.` Paragraph

The measurements in Table 5.04 were recorded on 20 November 2026 in the author's local development environment (Apple M2 Pro, 16.0 GB RAM, macOS 27.2.1, openjdk version "25.0.3" 2026-04-21 LTS, Dockerized PostgreSQL 18.1 (Debian 18.1-1.pgdg13+2) with PostGIS 3.6 USE_GEOS=1 USE_PROJ=1 USE_STATS=1, Redis error: NOPERM User quickbite has no permissions to run the 'info' command, and local OSRM [reachable (HTTP 200)]). HTTP endpoint scenarios report median (p50) and 95th-percentile (p95) latencies across 30 timed requests per condition after warm-up, toggling cold runs by clearing the target Redis keys (`quickbite:route:*`, `quickbite:restaurant_menu*`, `quickbite:restaurant_profile*`). Database query-plan scenarios (Rows 2–5) were executed via PostgreSQL `EXPLAIN (ANALYZE, BUFFERS)` inside rolled-back transactions containing 100,000 synthetic orders and 2,000 synthetic delivery agents, reporting median execution time across 5 runs with index scans disabled for unindexed baselines. Row 7 reports the analytical Little's Law connection-pool bound rather than a multi-node production load test, and Row 8 is derived directly from the configured scheduler intervals in `application.properties`. These figures reflect local test conditions and are not production service-level guarantees.

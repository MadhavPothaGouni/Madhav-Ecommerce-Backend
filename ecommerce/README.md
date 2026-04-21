# E-Commerce Microservices Platform

Spring Boot microservices with centralized security, service-to-service Feign clients,
distributed tracing (Zipkin + Micrometer), centralized logging (ELK), and
centralized configuration (Spring Cloud Config).

---

## Architecture Overview

```
Client → API Gateway (8080)
           ├── JWT Auth Filter (custom GlobalFilter)
           ├── Rate Limiter (Redis-backed)
           ├── Circuit Breaker (Resilience4j)
           └── Routes → User (8081) | Product (8082) | Order (8083) | Payment (8084) | Notification (8085)

Platform:
  Config Server  (8888) ← Git repo / filesystem
  Eureka Server  (8761) ← Service registry
  Zipkin         (9411) ← Distributed traces
  Prometheus     (9090) + Grafana (3000) ← Metrics
  ELK Stack      Elasticsearch (9200) + Logstash (5000) + Kibana (5601)
  RabbitMQ       (5672) ← Async events (order → notification)
  Redis          (6379) ← Gateway rate-limit + product cache
```

---

## Service Port Map

| Service              | Port |
|----------------------|------|
| API Gateway          | 8080 |
| User Service         | 8081 |
| Product Service      | 8082 |
| Order Service        | 8083 |
| Payment Service      | 8084 |
| Notification Service | 8085 |
| Config Server        | 8888 |
| Eureka Server        | 8761 |
| Zipkin               | 9411 |
| Prometheus           | 9090 |
| Grafana              | 3000 |
| Kibana               | 5601 |
| Elasticsearch        | 9200 |
| RabbitMQ Management  | 15672 |
| Redis                | 6379 |

---

## Startup Order

1. `config-server` — must be up first
2. `eureka-server` — registers with config
3. Infrastructure: MySQL ×5, Redis, RabbitMQ, Elasticsearch, Logstash, Zipkin
4. `api-gateway` — needs Eureka + Redis
5. Business services: `user-service`, `product-service`, `order-service`, `payment-service`, `notification-service`

---

## Quick Start (Docker Compose)

```bash
# Build all services
mvn clean package -DskipTests

# Start everything
docker compose up -d

# Tail logs
docker compose logs -f api-gateway order-service

# Stop everything
docker compose down -v
```

---

## Running Locally (without Docker)

```bash
# Start infra with Docker
docker compose up -d mysql-users mysql-products mysql-orders mysql-payments mysql-notifications \
  redis rabbitmq elasticsearch logstash kibana zipkin prometheus grafana

# Start platform services
cd config-server  && mvn spring-boot:run &
cd eureka-server  && mvn spring-boot:run &

# Start business services
cd user-service         && mvn spring-boot:run &
cd product-service      && mvn spring-boot:run &
cd order-service        && mvn spring-boot:run &
cd payment-service      && mvn spring-boot:run &
cd notification-service && mvn spring-boot:run &

# Start gateway last
cd api-gateway && mvn spring-boot:run
```

---

## Key Features Implemented

### 1. Centralized Security — API Gateway JWT Filter
- `JwtAuthenticationFilter` (GlobalFilter, Order=1) validates JWT on every request
- Public paths bypass auth: `/api/users/auth/**`, `/api/products/public/**`
- Extracts `userId`, `role`, `username` from JWT and forwards as `X-User-*` headers
- Role-based access: `/api/admin/**` requires `ROLE_ADMIN`
- Returns structured JSON errors (401/403) — no Spring Security on the gateway

### 2. Service-to-Service Communication — Feign Clients
- `order-service` → `product-service`: fetch products, deduct stock
- `order-service` → `payment-service`: initiate payment
- `FeignClientConfig` propagates `X-User-*` and `X-B3-*` trace headers automatically
- Fallback implementations for circuit-breaker degradation
- Resilience4j circuit breakers per service

### 3. Distributed Tracing — Zipkin + Micrometer Brave
- `micrometer-tracing-bridge-brave` + `zipkin-reporter-brave` in every service
- Trace sampling: 100% (configurable via `management.tracing.sampling.probability`)
- Trace/Span IDs automatically injected into MDC → appear in every log line
- View traces at http://localhost:9411

### 4. Centralized Logging — ELK Stack
- `logback-spring.xml` in every service: console + Logstash TCP appender (port 5000)
- `LogstashEncoder` sends structured JSON logs with `traceId`, `spanId`, `app`, `env` fields
- Logstash pipeline filters and ships to Elasticsearch index `ecommerce-logs-<service>-<date>`
- View and search logs at http://localhost:5601 (Kibana)

### 5. Centralized Configuration — Spring Cloud Config
- `config-server` serves YAML from Git (or local filesystem for dev)
- All services bootstrap via `spring.config.import: configserver:http://...`
- Shared `application.yml` in config repo → JWT secret, Eureka URL, JPA settings
- Service-specific overrides: `user-service.yml`, `order-service.yml`, etc.

### 6. Rate Limiting — Redis RequestRateLimiter
- Per-route rate limits configured in gateway `application.yml`
- Key: `X-User-Id` header if authenticated, remote IP otherwise
- Payment route: 5 req/s; Product route: 50 req/s; Order route: 10 req/s

### 7. Circuit Breaker + Fallback
- Resilience4j circuit breakers on every gateway route
- Feign client fallbacks per downstream service
- Dead-letter queue for failed notification messages

### 8. Async Events — RabbitMQ
- Order events (`order.placed`, `order.paid`, `order.cancelled`) published to `ecommerce.order.exchange`
- `notification-service` consumes from `notification.order.queue` with manual ACK
- Failed messages route to dead-letter queue `notification.order.dlq`

---

## API Quick Reference

### Auth
```
POST /api/users/auth/register   — register
POST /api/users/auth/login      — login → returns JWT
```

### Products (public)
```
GET /api/products/public/all
GET /api/products/public/search?keyword=phone
GET /api/products/public/category/Electronics
```

### Orders (authenticated)
```
POST   /api/orders              — create order
GET    /api/orders/my-orders    — list my orders
GET    /api/orders/{id}         — get order
PATCH  /api/orders/{id}/cancel  — cancel order
```

### Payments
```
GET /api/payments/my-payments   — my payment history
GET /api/payments/order/{id}    — payment for order
POST /api/payments/{id}/refund  — refund
```

---

## Kibana — Setting Up Index Pattern

1. Open http://localhost:5601
2. Stack Management → Index Patterns → Create
3. Pattern: `ecommerce-logs-*`
4. Time field: `@timestamp`
5. Filter by `traceId` field to correlate across services

---

## Grafana — Dashboards

1. Open http://localhost:3000 (admin/admin)
2. Datasource `Prometheus` is auto-provisioned
3. Import JVM Micrometer dashboard: ID `4701`
4. Import Spring Boot Statistics: ID `6756`

---

## Environment Variables for Production

| Variable                        | Description                    |
|---------------------------------|--------------------------------|
| `JWT_SECRET`                    | JWT signing key (min 32 chars) |
| `SPRING_DATASOURCE_PASSWORD`    | DB password                    |
| `SPRING_RABBITMQ_PASSWORD`      | RabbitMQ password              |
| `SPRING_MAIL_PASSWORD`          | SMTP app password              |
| `MANAGEMENT_ZIPKIN_TRACING_ENDPOINT` | Zipkin collector URL      |

---

## Project Structure

```
ecommerce/
├── pom.xml                        ← Parent POM (all modules)
├── docker-compose.yml             ← Full stack incl. ELK, Zipkin, Grafana
├── Dockerfile                     ← Multi-stage (copied to each service)
├── config-repo/
│   └── application.yml            ← Shared centralized config
├── elk/
│   └── logstash/
│       ├── pipeline/logstash.conf
│       └── config/logstash.yml
├── monitoring/
│   ├── prometheus.yml
│   └── grafana/provisioning/datasources/datasources.yml
├── config-server/
├── eureka-server/
├── api-gateway/
│   └── src/main/java/com/ecommerce/
│       ├── filter/JwtAuthenticationFilter.java  ← Core security
│       ├── filter/RequestLoggingFilter.java
│       ├── security/JwtUtil.java
│       └── config/RateLimiterConfig.java
├── user-service/
├── product-service/
├── order-service/
│   └── src/main/java/com/ecommerce/
│       ├── client/ProductClient.java   ← Feign
│       ├── client/PaymentClient.java   ← Feign
│       └── service/OrderService.java   ← Orchestration
├── payment-service/
└── notification-service/
    └── service/OrderEventConsumer.java ← RabbitMQ consumer
```

# Crypto Recommendation Service

A **production-ready** Spring Boot 3 (Java 21) service that imports crypto prices from CSV into an in-memory database (H2) and exposes recommendation endpoints.

## Tech stack

- **Java:** 21
- **Framework:** Spring Boot 3
- **Build tool:** Maven (multi-module)
- **Database:** H2 (in-memory, for the task)
- **API docs:** OpenAPI/Swagger UI (springdoc)
- **Observability:** Actuator + request correlation (`X-Request-Id` + MDC)
- **Rate limiting:** IP-based (Bucket4j)

## Prerequisites (local)

- JDK **21**
- Maven **3.9+** (or use `./mvnw`)
- Git
- Docker 
- k8s: minikube


## What is implemented

### Features
- **CSV import on startup** (no CSV reads per request)
- Stores price points in **H2** with indexes for `(symbol, timestamp)`
- Calculates:
  - `oldest/newest/min/max` for a crypto in a time range
  - normalized range per crypto: `(max - min) / min`
  - best crypto (highest normalized range) for a specific day
- **Caching** (Caffeine) for recommendations/stats/best/supported symbols
- **RFC7807 errors** (`application/problem+json`) and OpenAPI **ApiProblem** schema + reusable responses
- **Actuator metrics** (`/actuator/metrics`, `/actuator/prometheus`)
- **JaCoCo gate ≥ 80%** and automated tests (unit + integration)

## Architecture

This project uses a lightweight **Ports & Adapters / Hexagonal** structure (kept pragmatic for the task):
- `adapters/in` – REST API, CSV importer
- `adapters/out` – JPA persistence
- `application` – orchestration/use-cases
- `domain` – models and domain services

Benefits:
- testable business logic (service + repository aggregates)
- clear separation of API/persistence concerns
- easy to add new modules later

## Modules
- `crypto-recommendation-app` – Spring Boot application (REST API, importer, persistence)

## Build & test (all modules)

```bash
mvn clean verify
```

## Run application

```bash
mvn -pl crypto-recommendation-app spring-boot:run
```

## Swagger UI / OpenAPI
- Swagger UI: http://localhost:8080/swagger-ui
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Actuator health: http://localhost:8080/actuator/health

## REST endpoints (examples)

### Supported symbols
```bash
curl -s http://localhost:8080/api/v1/cryptos/supported
```

### Recommendations (normalized range sorted desc)
```bash
curl -s "http://localhost:8080/api/v1/cryptos/recommendations?from=2026-01-01&to=2026-01-31"
```

### Stats for crypto
```bash
curl -s "http://localhost:8080/api/v1/cryptos/BTC/stats?from=2026-01-01&to=2026-01-31"
```

### Best crypto for day
```bash
curl -s "http://localhost:8080/api/v1/cryptos/best?day=2026-01-01"
```

## Error format (problem+json)
Errors are returned as `application/problem+json` and documented as `ApiProblem` in OpenAPI.

Example:
```json
{
  "type": "https://example.com/problems/unsupported-crypto",
  "title": "Unsupported crypto symbol",
  "status": 404,
  "detail": "Crypto symbol 'DOGE' is not supported",
  "symbol": "DOGE"
}
```

## Actuator / metrics
- Health: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/metrics
- Prometheus: http://localhost:8080/actuator/prometheus

## CSV import

### Expected format
Header (case-insensitive): `timestamp,symbol,price`

Example:
```csv
timestamp,symbol,price
1641009600000,BTC,46813.21
```

### Where to put files
Place `*_values.csv` files under:
- `crypto-recommendation-app/src/main/resources/data/`

Import runs at startup. Configure via `application.yml`:
- `app.import.enabled`
- `app.import.clean-before-import`
- `app.import.resource-pattern`

## Potential enhancements
- Persist into a real DB (PostgreSQL) and use Flyway migrations
- Precompute monthly aggregates in a separate table for faster queries
- Add pagination and filtering for recommendations
- Add security (Okta tokens) for endpoints + Security for Actuator
- Add support for longer periods (6 months / 1 year) via configurable range presets
- Migrate rate limit to use Bucket4j and Redis to handle running app on several pods

## Container image

### Build docker image
```bash
docker build -t crypto-recommendation:latest .
```

### Run Application only on docker 
```bash
docker run --rm -p 8080:8080 crypto-recommendation:latest
```

## Kubernetes

Start Minikube (Docker driver) and load the image: 
```bash
#when working on WSL Ubuntu on Windows - error(GDBus.Error:org.freedesktop.DBus.Error.ServiceUnknown: The name org.freedesktop.secrets was not provided by any .service)
sudo apt install gnome-keyring

# first start minikube
minikube version
kubectl version --client
minikube start

# check if cluster has started
kubectl get nodes

# check if image is loaded 
docker images | grep crypto-recommendation

# load image to minikube
minikube image load crypto-recommendation:latest

# force reload image - only code update - need do build new docker image
kubectl set image deploy/crypto-recommendation app=crypto-recommendation:latest_v20260130

# force reload image on code update - check status
kubectl rollout status deployment/crypto-recommendation
```

### Deploy to Minikube
```bash
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

```bash
# check if pod is running - need to wait for start
kubectl get pods -w

#enable port forwarding to be able to reach application through localhost:8080
kubectl port-forward svc/crypto-recommendation 8080:80

# get pod endpoint
minikube service crypto-recommendation --url

# check pod logs
kubectl logs -l app=crypto-recommendation --tail=200

# reload application on minikube - on property change
kubectl rollout restart deployment/crypto-recommendation

# reload application on minikube - check status 
kubectl rollout status deployment/crypto-recommendation
```


## Rate limiting (IP-based)

The service includes an **application-level** IP-based rate limiter (Bucket4j). It is enabled by default and can be configured:

- `app.rate-limit.enabled` (default: true)
- `app.rate-limit.capacity` (default: 12)
- `app.rate-limit.refill-tokens` (default: 120)
- `app.rate-limit.refill-duration` (default: PT1M)

When running behind an Ingress/Load Balancer, the service uses `X-Forwarded-For` (first IP) to identify the client.
For multi-replica setups - considered distributed rate limiting (e.g., Redis).


## Configuration & environments

The service is configured using **Spring profiles** and **environment variables**.

### Profiles
- `dev` (default during local development): H2 console enabled
- `prod`: H2 console disabled, graceful shutdown enabled, virtual threads enabled

Run with a profile:
```bash
SPRING_PROFILES_ACTIVE=dev mvn -pl crypto-recommendation-app spring-boot:run
SPRING_PROFILES_ACTIVE=prod mvn -pl crypto-recommendation-app spring-boot:run
```

### Environment variables
Spring Boot maps environment variables to configuration properties automatically (e.g. `SPRING_DATASOURCE_URL`).
Custom application variables are provided with sensible defaults and can be overridden:

- Import:
  - `APP_IMPORT_ENABLED`
  - `APP_IMPORT_CLEAN_BEFORE_IMPORT`
  - `APP_IMPORT_RESOURCE_PATTERN`
  - `APP_IMPORT_BATCH_SIZE`

- Rate limiting:
  - `APP_RATE_LIMIT_ENABLED`
  - `APP_RATE_LIMIT_CAPACITY`
  - `APP_RATE_LIMIT_REFILL_TOKENS`
  - `APP_RATE_LIMIT_REFILL_DURATION`
  - `APP_RATE_LIMIT_MAX_BUCKETS`
  - `APP_RATE_LIMIT_BUCKET_EXPIRE_AFTER_ACCESS`

Kubernetes examples are included in `k8s/configmap.yaml` and `k8s/secret.yaml`.

### Production note
H2 is used for the test task. For real production, I would use an external database (e.g. PostgreSQL) and schema migrations (Flyway).


### Troubleshooting Docker build
**minikube**
```bash
minikube start
eval $(minikube docker-env)
docker build -t crypto-recommendation:latest .
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

### Port-forwarding
```bash
kubectl port-forward svc/crypto-recommendation 8080:80
```

Then open:
- http://localhost:8080/swagger-ui
- http://localhost:8080/v3/api-docs

And postman collection attached to the project should also work 
[Postman collection](postman-collection/Crypto Recommendations.json)


## Setup only when kubectl + Minikube on WSL (Ubuntu) are missing 

### Install kubectl

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl

curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
rm kubectl

kubectl version --client
```

### Install Minikube

```bash
curl -LO https://github.com/kubernetes/minikube/releases/latest/download/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube
rm minikube-linux-amd64

minikube version
```
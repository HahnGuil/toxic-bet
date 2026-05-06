# Toxic Bet API

## Portuguese Version

For the Portuguese version of this README, see [`README_PT.md`](./README_PT.md).

---

## Overview

**Toxic Bet API** is the main backend for a World Cup 2026 betting pool application. It manages application users, championships, teams, matches, bets, odds, betting pools, and score calculation.

The service is built with **Java 21** and **Spring Boot 3.4.11**, using a reactive stack with **WebFlux**, **R2DBC/PostgreSQL**, **Flyway**, JWT validation through an external Auth-Server JWK set, and Kafka events for OAuth user synchronization.

---

## Technologies

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.11-6DB33F?style=flat&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-316192?style=flat&logo=postgresql&logoColor=white)
![Kafka](https://img.shields.io/badge/Kafka-integrated-231F20?style=flat&logo=apachekafka&logoColor=white)
![JUnit](https://img.shields.io/badge/JUnit-5-25A162?style=flat&logo=junit5&logoColor=white)

- **Java 21** as defined in `pom.xml`.
- **Spring Boot 3.4.11** as the application foundation.
- **Spring WebFlux** for reactive REST endpoints and SSE.
- **Spring Security + OAuth2 Resource Server** for JWT bearer protection.
- **Spring Data R2DBC** for reactive PostgreSQL access.
- **Flyway** for database migrations.
- **Apache Kafka** for `sync-application` events.
- **Caffeine Cache** for configured runtime caches.
- **Springdoc OpenAPI** for Swagger/OpenAPI documentation.
- **JUnit 5 + Mockito + Reactor Test** for tests.

---

## Features

### Users
- Check whether a user already exists by email.
- Register an application user after authentication in the external Auth-Server.
- Retrieve users by ID or email.
- Publish OAuth user synchronization events through Kafka when needed.

### Matches
- Create matches with `ADMIN` role validation.
- Validate future match dates.
- Prevent team schedule conflicts inside a 3-hour window.
- Open matches for betting.
- Close betting for a match.
- Finish matches with the final score.
- Automatically move eligible matches to `IN_PROGRESS`.
- Stream match lists through Server-Sent Events.

### Bets
- Register one bet per user per match.
- Block bets for matches that are not open.
- Process bets sequentially by match to reduce lock contention.
- Calculate dynamic odds and user points.
- Recalculate scores when matches finish.

### Betting Pools
- Create betting pools.
- Find betting pools by unique code.
- Join a shared betting pool.
- List betting pool users ordered by score.

### Integrations
- Validate JWT tokens using the Auth-Server JWK set.
- Publish Kafka events to `sync-application`.
- Provide OpenAPI documentation generated from `src/main/resources/static/swagger.yml`.

---

## Architecture

The project follows a layered structure:

- `src/main/java/br/com/hahn/toxicbet/application/`
  - `controller/` - REST controllers.
  - `service/` - business rules.
  - `mapper/` - entity/DTO mapping.
- `src/main/java/br/com/hahn/toxicbet/domain/`
  - `model/` - domain entities and value objects.
  - `repository/` - reactive repositories.
  - `exception/` - business exceptions.
- `src/main/java/br/com/hahn/toxicbet/infrastructure/`
  - `config/` - technical configuration.
  - `security/` - JWT security.
  - `service/` - technical integrations.
  - `scheduling/` - scheduled jobs.
- `src/main/resources/`
  - `application-local.yml`
  - `application-docker.yml`
  - `application-aws.yml`
  - `db/migration/`
  - `static/swagger.yml`

---

## API References

### Runtime URLs

- Local API: [`http://localhost:10000`](http://localhost:10000)
- Docker API: [`http://localhost:20000`](http://localhost:20000)
- Production API: [`https://api.toxicbet.com.br`](https://api.toxicbet.com.br)

### OpenAPI and Swagger

- Local Swagger UI: [`http://localhost:10000/swagger-ui.html`](http://localhost:10000/swagger-ui.html)
- Local Swagger UI alternative: [`http://localhost:10000/swagger-ui/index.html`](http://localhost:10000/swagger-ui/index.html)
- Docker Swagger UI: [`http://localhost:20000/swagger-ui.html`](http://localhost:20000/swagger-ui.html)
- Local OpenAPI JSON: [`http://localhost:10000/v3/api-docs`](http://localhost:10000/v3/api-docs)
- Docker OpenAPI JSON: [`http://localhost:20000/v3/api-docs`](http://localhost:20000/v3/api-docs)
- Static OpenAPI source: [`src/main/resources/static/swagger.yml`](src/main/resources/static/swagger.yml)

### Related APIs

- Auth-Server Docker API: [`http://localhost:2300/auth-server`](http://localhost:2300/auth-server)
- Auth-Server local API: [`http://localhost:2310/auth-server`](http://localhost:2310/auth-server)
- Auth-Server production API: [`https://auth.toxicbet.com.br/auth-server`](https://auth.toxicbet.com.br/auth-server)
- Auth-Server JWK set: [`https://auth.toxicbet.com.br/auth-server/public-key/jwks`](https://auth.toxicbet.com.br/auth-server/public-key/jwks)
- Frontend local URL: [`http://localhost:4200`](http://localhost:4200)

### Main Documented Endpoints

- **Users**
  - `GET /users/existsByEmail/`
  - `POST /users`
  - `GET /users`
- **Match**
  - `POST /match`
  - `GET /match` (SSE)
  - `GET /match/find-open` (SSE)
  - `GET /match/in-progress` (SSE)
  - `PATCH /match/end-bet`
  - `PATCH /match`
  - `PATCH /match/open`
- **Bet**
  - `POST /bet`
  - `GET /bet/results`
  - `GET /bet/results/open`
- **Betting Pool**
  - `POST /bettingPool`
  - `GET /bettingPool/{bettingPoolKey}`
  - `PATCH /bettingPool/{bettingPoolKey}`
  - `GET /bettingPool/getUsers`

---

## Environment Variables

The service supports three execution profiles:

- `local` - application runs on the host and uses a local PostgreSQL.
- `docker` - application and PostgreSQL run in containers.
- `aws` - application runs in a container and consumes external AWS dependencies.

Use `.env.example` as the base file.

| Variable | Description |
|---|---|
| `POSTGRES_LOCAL_HOST` | PostgreSQL host for the `local` profile |
| `POSTGRES_LOCAL_PORT` | PostgreSQL port for the `local` profile |
| `POSTGRES_LOCAL_USER` | PostgreSQL user for the `local` profile |
| `POSTGRES_LOCAL_PASSWORD` | PostgreSQL password for the `local` profile |
| `POSTGRES_LOCAL_DB` | PostgreSQL database for the `local` profile |
| `POSTGRES_DOCKER_HOST` | PostgreSQL host for the `docker` profile |
| `POSTGRES_DOCKER_PORT` | PostgreSQL port for the `docker` profile |
| `POSTGRES_DOCKER_USER` | PostgreSQL user for the `docker` profile |
| `POSTGRES_DOCKER_PASSWORD` | PostgreSQL password for the `docker` profile |
| `POSTGRES_DOCKER_DB` | PostgreSQL database for the `docker` profile |
| `POSTGRES_AWS_HOST` | PostgreSQL host for the `aws` profile |
| `POSTGRES_AWS_PORT` | PostgreSQL port for the `aws` profile |
| `POSTGRES_AWS_USER` | PostgreSQL user for the `aws` profile |
| `POSTGRES_AWS_PASSWORD` | PostgreSQL password for the `aws` profile |
| `POSTGRES_AWS_DB` | PostgreSQL database for the `aws` profile |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers for Docker |
| `KAFKA_AWS_BOOTSTRAP_SERVERS` | Kafka bootstrap servers for AWS |
| `AUTH_SERVER_JWK_SET_URI` | Auth-Server JWK endpoint used to validate JWT tokens |
| `AUTH_SERVER_BASE_URL` | Auth-Server base URL used for service integrations |
| `SHARED_SERVICES_NETWORK` | Docker network shared with backend services |

Example:

```dotenv
POSTGRES_DOCKER_HOST=postgres
POSTGRES_DOCKER_PORT=5432
POSTGRES_DOCKER_USER=postgres
POSTGRES_DOCKER_PASSWORD=postgres
POSTGRES_DOCKER_DB=toxic-bet
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
AUTH_SERVER_JWK_SET_URI=http://ms-auth-server:2300/auth-server/public-key/jwks
AUTH_SERVER_BASE_URL=http://ms-auth-server:2300
SHARED_SERVICES_NETWORK=shared-services
```

---

## Docker

The repository has dedicated Docker Compose files per profile:

- `compose.local.yaml` - starts only PostgreSQL for local profile support.
- `compose.docker.yaml` - starts API + PostgreSQL for the `docker` profile.
- `compose.aws.yaml` - starts only the API with the `aws` profile, assuming external infrastructure.

Kafka and Auth-Server are not managed by this repository. In the `docker` profile, they must be reachable through the shared Docker network.

### 1. Create `.env`

```bash
cp .env.example .env
```

### 2. Create the shared network

```bash
docker network create shared-services
```

If your network has another name, update `SHARED_SERVICES_NETWORK` in `.env`.

### 3. Start the desired profile

```bash
docker compose -f compose.local.yaml up -d
docker compose -f compose.docker.yaml up -d --build
docker compose -f compose.aws.yaml up -d --build
```

### 4. Check containers

```bash
docker compose -f compose.local.yaml ps
docker compose -f compose.docker.yaml ps
docker compose -f compose.aws.yaml ps
```

### 5. Stop services

```bash
docker compose -f compose.local.yaml down
docker compose -f compose.docker.yaml down
docker compose -f compose.aws.yaml down
```

---

## Running Locally

### Prerequisites

- Java 21
- Docker, optional for PostgreSQL
- Kafka reachable from the configured `KAFKA_BOOTSTRAP_SERVERS`
- Auth-Server reachable from the configured `AUTH_SERVER_JWK_SET_URI`

### 1. Start local PostgreSQL, if needed

```bash
docker compose -f compose.local.yaml up -d
```

### 2. Export environment variables

```bash
export POSTGRES_LOCAL_HOST="localhost"
export POSTGRES_LOCAL_PORT="5435"
export POSTGRES_LOCAL_USER="postgres"
export POSTGRES_LOCAL_PASSWORD="postgres"
export POSTGRES_LOCAL_DB="toxic-bet-local"
export AUTH_SERVER_JWK_SET_URI="http://localhost:2310/auth-server/public-key/jwks"
export AUTH_SERVER_BASE_URL="http://localhost:2310"
```

### 3. Run the application

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The local profile exposes the API on port `10000`.

---

## Security

The API is protected with JWT bearer tokens.

### Public Paths

- `/actuator/**`
- `/public/**`
- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`

### Protected Paths

All other endpoints require a valid JWT issued by a server compatible with the configured `spring.security.oauth2.resourceserver.jwt.jwk-set-uri`.

Match management operations require the `ADMIN` role.

---

## Database and Migrations

Migrations are stored in `src/main/resources/db/migration/`.

Current migrations:

- `V1__create_initial_tables.sql`
- `V2__create_role_colum_users.sql`

Main structures:

- `users`
- `teams`
- `championship`
- `championship_teams`
- `match`
- `bet`
- `betting_pool`

Relevant rules:

- `users.email` is unique.
- `bet` has a unique constraint for `(user_id, match_id)`.
- `users.role` supports `USER` and `ADMIN`.
- `match` stores odds, bet totals, and match status.
- A database trigger prevents bet insertion after the match starts.

---

## Scheduled Jobs

`ApplicationScheduler` opens all `NOT_STARTED` matches for the current day at midnight and updates open matches to `IN_PROGRESS` when their start time has passed.

---

## Tests

Run the test suite with:

```bash
./mvnw test
```

The current test suite covers:

- `UserService`
- `TeamService`
- `ChampionshipService`
- `OddsService`
- `BetProcessorServiceLoadTest`

---

## Important Files

- `pom.xml` - dependencies, plugins, and build settings.
- `compose.local.yaml` - PostgreSQL for the local profile.
- `compose.docker.yaml` - API + PostgreSQL for the Docker profile.
- `compose.aws.yaml` - API for the AWS profile.
- `.env.example` - base environment variables.
- `src/main/resources/application-local.yml` - local profile.
- `src/main/resources/application-docker.yml` - Docker profile.
- `src/main/resources/application-aws.yml` - AWS profile.
- `src/main/resources/db/migration/` - Flyway migrations.
- `src/main/resources/static/swagger.yml` - OpenAPI contract.

---

## License

No license file was identified in this repository.

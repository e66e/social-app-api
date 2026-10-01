# Social App API

A modular social networking REST API built with **Spring Boot** and **Spring Modulith**, providing core social features such as users, posts, comments, notifications, and onboarding. The API is secured with OAuth2/JWT (Keycloak) and uses PostgreSQL for persistence. It includes idempotency for post creation, Flyway migrations, and a Docker Compose setup for local development.

---

## ✨ Features

- **User management** – registration, profile handling, and authentication via Keycloak.
- **Posts** – create, retrieve, and manage posts with **idempotency** support (safe retries).
- **Comments** – attach comments to posts.
- **Notifications** – event‑driven notifications powered by Spring Modulith’s event system.
- **Onboarding** – guided setup flow for new users.
- **API versioning** – version negotiated via the `X-Version` header (defaults to `1.0.0`).
- **Modular architecture** – enforced module boundaries using Spring Modulith.
- **Database migrations** – Flyway‑managed schema evolution.
- **Docker‑first** – one‑command local environment with PostgreSQL and Keycloak.

---

## 🧱 Tech Stack

| Category | Technology |
|----------|------------|
| Language | Java 25 |
| Framework | Spring Boot 4.1.0, Spring Modulith 2.1.0 |
| Persistence | Spring Data JPA, PostgreSQL, Flyway |
| Security | Spring Security OAuth2 Resource Server (JWT), Keycloak |
| Mapping | MapStruct 1.6.3 |
| Boilerplate | Lombok |
| Testing | JUnit 5, Mockito, Testcontainers, Spring Modulith Test |
| Build | Gradle |
| Container | Docker, Docker Compose |

---

## 🏗️ Architecture

The application follows a **modular monolith** structure. Each business domain is encapsulated in its own module under `src/main/java/dev/e66e/social_app_api/`:

```
social_app_api/
├── comments/        # Comment entity, DTO, mapper, persistence
├── config/          # Cross‑cutting configuration (security, etc.)
├── gateway/         # Entry points / adapters
├── notifications/   # Notification logic
├── onboarding/      # User onboarding flow
├── posts/           # Post management (including idempotency)
├── users/           # User domain
└── SocialAppAPIApplication.java
```

Modules interact only through public APIs, and Spring Modulith verifies these boundaries at build time.

---

## 📋 Prerequisites

- **Java 25** (the project uses a Java toolchain; Gradle will download it if needed).
- **Docker & Docker Compose** (for the provided local environment).
- **Gradle** (or use the included `./gradlew` wrapper).

---

## 🚀 Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/e66e/social-app-api.git
cd social-app-api
```

### 2. Start the supporting services (PostgreSQL + Keycloak)

The `docker/compose.yaml` file defines the required infrastructure. Spring Boot’s Docker Compose support can start it automatically when you run the application. Alternatively, start it manually:

```bash
docker compose -f docker/compose.yaml up -d
```

This launches:
- **PostgreSQL** on `localhost:5432` (database `social_app_db`, user `admin`, password `secret`).
- **Keycloak** on `localhost:8090` with a pre‑configured realm `social-app` and client `social-app-api`.

### 3. Build the project

```bash
./gradlew build
```

### 4. Run the application

```bash
./gradlew bootRun
```

The API will be available at **`http://localhost:8080/api`**.

---

## ⚙️ Configuration

Key settings are in `src/main/resources/application.yaml`:

| Property | Description |
|----------|-------------|
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | Keycloak issuer URL (`http://localhost:8090/realms/social-app`) |
| `spring.datasource.url` | PostgreSQL JDBC URL (`jdbc:postgresql://localhost:5432/social_app_db`) |
| `spring.datasource.username` / `password` | Database credentials (`admin` / `secret`) |
| `spring.flyway.*` | Flyway migration settings |
| `spring.jpa.hibernate.ddl-auto` | Set to `validate` – schema is managed by Flyway |
| `spring.modulith.events.republish-outstanding-events-on-restart` | `true` – ensures reliable event delivery |
| `server.servlet.context-path` | `/api` – all endpoints are prefixed with `/api` |
| `spring.mvc.apiversion.*` | API version negotiation via `X-Version` header (default `1.0.0`) |

You can override any of these via environment variables or an `application-local.yaml` file.

---

## 📚 API Documentation

The API is versioned and all endpoints live under `/api`. Below is a high‑level overview.

| Module | Base Path | Description |
|--------|-----------|-------------|
| Users | `/api/users` | User CRUD and profile management |
| Posts | `/api/posts` | Post creation, retrieval, and deletion (idempotent creation) |
| Comments | `/api/comments` | Comment management per post |
| Notifications | `/api/notifications` | Fetch and mark notifications |
| Onboarding | `/api/onboarding` | Onboarding steps and completion |

**Authentication:** All endpoints (except public health checks) require a valid JWT issued by the configured Keycloak realm. Include it in the `Authorization: Bearer <token>` header.

**Idempotency for post creation:**  
`POST /api/posts` accepts an optional `Idempotency-Key` header. If the same key is used within a configurable window, the API returns the original response instead of creating a duplicate post.

> A full OpenAPI/Swagger specification can be generated via SpringDoc (add `springdoc-openapi-starter-webmvc-ui` if not already present). The project does not currently include a static OpenAPI file.

---

## 🧪 Testing

Run the test suite with:

```bash
./gradlew test
```

Tests use **Testcontainers** to spin up ephemeral PostgreSQL instances, ensuring integration tests run against a real database. Spring Modulith’s test support verifies module boundaries and event flows.

---

## 📁 Project Structure (top‑level)

```
social-app-api/
├── docker/                 # Docker Compose, Keycloak realm, init scripts
├── gradle/wrapper/         # Gradle wrapper
├── src/
│   ├── main/
│   │   ├── java/dev/e66e/social_app_api/   # Application modules
│   │   └── resources/
│   │       ├── db/migration/               # Flyway SQL migrations
│   │       └── application.yaml
│   └── test/                               # Unit & integration tests
├── build.gradle
├── gradlew
├── settings.gradle
└── README.md
```

---

## 📄 License

This project is not currently associated with a specific license. If you intend to use it, feel free to do so.

---

## 🙏 Acknowledgements

- [Spring Modulith](https://spring.io/projects/spring-modulith)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Keycloak](https://www.keycloak.org/)
- [Testcontainers](https://testcontainers.com/)
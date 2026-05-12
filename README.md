# 🖊️ Inkwell Blogging — Backend

> A production-ready, microservices-based blogging platform backend built with **Spring Boot**, **Spring Cloud**, **RabbitMQ**, **Redis**, and **MySQL**.

---

## 📑 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Microservices](#microservices)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Infrastructure Setup (Docker)](#infrastructure-setup-docker)
- [Service Run Order](#service-run-order)
- [Environment Variables](#environment-variables)
- [API Gateway & JWT Auth](#api-gateway--jwt-auth)
- [OAuth2 Social Login](#oauth2-social-login)
- [Testing](#testing)
- [API Documentation (Swagger)](#api-documentation-swagger)
- [Database Reset](#database-reset)
- [Project Structure](#project-structure)

---

## Overview

**Inkwell** is a full-featured blogging platform backend. It follows a **microservices architecture** where each domain concern is handled by an independent Spring Boot service. All services register with **Eureka**, communicate via **REST** (synchronously) and **RabbitMQ** (asynchronously), and are accessed through a single **API Gateway**.

---

## Architecture

```
                        ┌─────────────────────────────┐
                        │         API Gateway          │
                        │        (Port: 8080)          │
                        │  - JWT Validation            │
                        │  - Route → Microservices     │
                        └──────────────┬──────────────┘
                                       │
              ┌────────────────────────┼────────────────────────┐
              │                        │                        │
   ┌──────────▼──────┐    ┌────────────▼────────┐   ┌──────────▼────────────┐
   │  auth-service   │    │ post-category-tag   │   │  comment-like-service │
   │  (Port: 8081)   │    │    (Port: 8082)     │   │     (Port: 8083)      │
   └─────────────────┘    └─────────────────────┘   └───────────────────────┘
   ┌─────────────────┐    ┌─────────────────────┐   ┌───────────────────────┐
   │  media-service  │    │ newsletter-service  │   │  notification-service │
   │  (Port: 8084)   │    │    (Port: 8085)     │   │     (Port: 8086)      │
   └─────────────────┘    └─────────────────────┘   └───────────────────────┘
              │                        │                        │
              └────────────────────────▼────────────────────────┘
                                       │
                    ┌──────────────────▼──────────────────┐
                    │           Eureka Server              │
                    │           (Port: 8761)               │
                    └──────────────────────────────────────┘

   Infrastructure:  MySQL (3306) | RabbitMQ (5672) | Redis (6379) | SMTP (2525)
```

---

## Microservices

| Service | Port | Responsibility |
|---|---|---|
| `eureka-server` | `8761` | Service discovery & registry |
| `api-gateway` | `8080` | Single entry point, JWT validation, routing |
| `auth-service` | `8081` | Users, roles, JWT, OAuth2, password reset |
| `post-category-tag-service` | `8082` | Posts, categories, tags, saved posts |
| `comment-like-service` | `8083` | Comments, comment likes |
| `media-service` | `8084` | File uploads & media metadata |
| `newsletter-subscription-service` | `8085` | Newsletter campaigns & subscriptions |
| `notification-service` | `8086` | In-app notifications |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| Service Discovery | Spring Cloud Netflix Eureka |
| API Gateway | Spring Cloud Gateway |
| Messaging | RabbitMQ 3.13 |
| Caching | Redis 7.2 |
| Database | MySQL 8.4 |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security + JWT |
| Social Login | Spring OAuth2 (Google, GitHub) |
| Mail | Spring Mail (SMTP / Gmail) |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Testing | JUnit 5 + Mockito |
| Build Tool | Maven (Maven Wrapper included) |
| Containerisation | Docker + Docker Compose |

---

## Prerequisites

Make sure the following are installed on your system:

- **Java 21** or higher
- **Maven** (or use the `mvnw` wrapper included in each service)
- **Docker & Docker Compose** (for infrastructure)
- **IntelliJ IDEA** (recommended) or any IDE supporting Spring Boot

---

## Infrastructure Setup (Docker)

All infrastructure dependencies (MySQL, RabbitMQ, Redis, SMTP) can be started with a single command:

```bash
# From the inkwell-blogging/ root directory
docker-compose up -d
```

### Services started by Docker:

| Container | Port | Description |
|---|---|---|
| `inkwell-mysql` | `3306` | MySQL 8.4 database |
| `inkwell-rabbitmq` | `5672` / `15672` | RabbitMQ + Management UI |
| `inkwell-redis` | `6379` | Redis cache |
| `inkwell-smtp4dev` | `2525` / `5000` | Local fake SMTP server |

> **RabbitMQ UI:** http://localhost:15672 (user: `guest`, pass: `guest`)
>
> **SMTP4dev UI:** http://localhost:5000 (view emails sent locally)

---

## Service Run Order

> ⚠️ **Services must be started in this exact order.**

```
1. Start Docker infrastructure (MySQL, RabbitMQ, Redis, SMTP)
2. Start eureka-server       → http://localhost:8761
3. Start (in any order):
   - auth-service            → http://localhost:8081
   - post-category-tag-service → http://localhost:8082
   - comment-like-service    → http://localhost:8083
   - media-service           → http://localhost:8084
   - newsletter-subscription-service → http://localhost:8085
   - notification-service    → http://localhost:8086
4. Start api-gateway         → http://localhost:8080
```

### Running a service (from its folder):

```bash
# Using Maven wrapper
./mvnw spring-boot:run

# Or if Maven is installed globally
mvn spring-boot:run
```

---

## Environment Variables

Each service reads configuration from `application.properties`. Override sensitive values using environment variables:

### auth-service

| Variable | Default | Description |
|---|---|---|
| `SMTP_HOST` | `smtp.gmail.com` | Mail server host |
| `SMTP_PORT` | `587` | Mail server port |
| `SMTP_USERNAME` | _(empty)_ | Gmail address |
| `SMTP_PASSWORD` | _(empty)_ | Gmail App Password |
| `AUTH_MAIL_FROM` | = `SMTP_USERNAME` | From address in emails |

### newsletter-subscription-service

| Variable | Description |
|---|---|
| `SMTP_HOST` | Mail server host |
| `SMTP_PORT` | Mail server port |
| `SMTP_USERNAME` | Sender Gmail address |
| `SMTP_PASSWORD` | Gmail App Password |
| `NEWSLETTER_MAIL_FROM` | Newsletter sender address |

> **Gmail setup tip:** Enable 2-Step Verification on your Google account → go to **App Passwords** → generate one for "Mail" → use it as `SMTP_PASSWORD`.

### Admin Registration

The default admin registration key is:

```
INKWELL_ADMIN_2026
```

Set it via `auth.admin.registration-key` in `auth-service/application.properties`.

---

## API Gateway & JWT Auth

- All protected API calls must include the `Authorization` header:

```http
Authorization: Bearer <your-jwt-token>
```

- The gateway validates JWT before forwarding requests to downstream services.
- Public routes (login, register, public posts) are excluded from JWT validation.

### Base URL (after gateway):

```
http://localhost:8080
```

---

## OAuth2 Social Login

OAuth2 is supported for **Google** and **GitHub** but is enabled only via a Spring profile.

### Setup:

1. Set the following environment variables before starting `auth-service`:

```bash
GOOGLE_CLIENT_ID=<your-google-client-id>
GOOGLE_CLIENT_SECRET=<your-google-client-secret>
GITHUB_CLIENT_ID=<your-github-client-id>
GITHUB_CLIENT_SECRET=<your-github-client-secret>
```

2. Start `auth-service` with the `oauth` profile:

```bash
# IntelliJ VM option
-Dspring.profiles.active=oauth

# Or as env var
SPRING_PROFILES_ACTIVE=oauth
```

3. On successful OAuth login, retrieve token from:

```
GET /api/auth/oauth2/success
```

---

## Testing

Unit tests are written using **JUnit 5** and **Mockito**. Each service has its own test suite.

### Run tests for a service:

```bash
# From inside that service's directory
./mvnw test
```

### Available test classes:

| Service | Test Class |
|---|---|
| `auth-service` | `AuthServiceUnitTest`, `AuthServiceFollowUnitTest` |
| `comment-like-service` | `CommentServiceUnitTest` |
| `post-category-tag-service` | `PostServiceSavedPostUnitTest` |
| `newsletter-subscription-service` | `NewsletterServiceUnitTest` |
| `notification-service` | `NotificationServiceUnitTest` |

### When to use JUnit vs Mockito:

- **JUnit** → Verify pure business logic, assertions, expected exceptions.
- **Mockito** → Mock repositories/external services, isolate the unit under test, verify interactions like `save()`, `delete()`.

---

## API Documentation (Swagger)

Each service exposes a Swagger UI at:

```
http://localhost:<PORT>/swagger-ui.html
```

| Service | Swagger URL |
|---|---|
| auth-service | http://localhost:8081/swagger-ui.html |
| post-category-tag-service | http://localhost:8082/swagger-ui.html |
| comment-like-service | http://localhost:8083/swagger-ui.html |
| media-service | http://localhost:8084/swagger-ui.html |
| newsletter-subscription-service | http://localhost:8085/swagger-ui.html |
| notification-service | http://localhost:8086/swagger-ui.html |

Raw OpenAPI JSON:

```
http://localhost:<PORT>/api-docs
```

You can also test APIs using the pre-written `.http` files inside the `http/` folder.

---

## Database Reset

If you encounter JPA schema mismatch errors (e.g., unknown column):

```
1. Stop all services
2. Run: database/reset-and-init-databases.sql (in your MySQL client)
3. Restart services — JPA will recreate the schema automatically
```

---

## Project Structure

```
inkwell-blogging/
├── docker-compose.yml                  # Infrastructure: MySQL, RabbitMQ, Redis, SMTP
├── RUN_ORDER.md                        # Detailed service startup guide
├── database/
│   ├── init-databases.sql              # Initial DB + schema setup
│   └── reset-and-init-databases.sql    # Reset & reinitialise all databases
├── http/                               # HTTP test files for API testing
├── eureka-server/                      # Service registry (Eureka)
├── api-gateway/                        # API Gateway (JWT + routing)
├── auth-service/                       # Authentication & user management
├── post-category-tag-service/          # Posts, categories, tags
├── comment-like-service/               # Comments & likes
├── media-service/                      # File uploads
├── newsletter-subscription-service/    # Newsletter & subscriptions
└── notification-service/               # Notifications
```

Each service follows a standard Spring Boot project layout:

```
<service-name>/
├── pom.xml
├── mvnw / mvnw.cmd
└── src/
    ├── main/
    │   ├── java/com/inkwell/<service>/
    │   │   ├── controller/
    │   │   ├── service/
    │   │   ├── repository/
    │   │   ├── model/
    │   │   ├── dto/
    │   │   ├── config/
    │   │   └── security/
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/com/inkwell/<service>/
```

---

## User Roles

| Role | Access |
|---|---|
| `READER` | Read posts, comment, save posts, manage profile |
| `AUTHOR` | Everything READER + create/edit/delete own posts, manage media |
| `ADMIN` | Full platform access: user management, post moderation, analytics, newsletter |

---



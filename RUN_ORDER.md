# InkWell Backend Run Order

1. Ensure local dependencies are running (without Docker):
   - MySQL on `localhost:3306`
   - RabbitMQ on `localhost:5672` (optional UI `http://localhost:15672`)
   - Redis on `localhost:6379` (if used by any service)
   - SMTP server:
     - For local testing: smtp4dev on `localhost:2525` (UI `http://localhost:5000`)
     - For real Gmail inbox delivery, set:
       - `SMTP_HOST=smtp.gmail.com`
       - `SMTP_PORT=587`
       - `SMTP_USERNAME=<your-gmail-id>`
       - `SMTP_PASSWORD=<gmail-app-password>`
       - `NEWSLETTER_MAIL_FROM=<same-gmail-id>`
     - Gmail account must have 2-Step Verification enabled, then create an App Password.
2. Start `eureka-server` (port `8761`).
3. Start services in any order:
   - `auth-service` (`8081`)
   - `post-category-tag-service` (`8082`)
   - `comment-like-service` (`8083`)
   - `media-service` (`8084`)
   - `newsletter-subscription-service` (`8085`)
   - `notification-service` (`8086`)
4. Start `api-gateway` (`8080`).
5. Test APIs from `http/*.http`.

## OAuth (Google/GitHub) local setup
- Set env vars before running `auth-service`:
  - `GOOGLE_CLIENT_ID`
  - `GOOGLE_CLIENT_SECRET`
  - `GITHUB_CLIENT_ID`
  - `GITHUB_CLIENT_SECRET`
- Start `auth-service` with profile `oauth` only when needed:
  - IntelliJ VM option: `-Dspring.profiles.active=oauth`
  - or env var: `SPRING_PROFILES_ACTIVE=oauth`
- OAuth success endpoint returns token:
  - `GET /api/auth/oauth2/success`

## If you hit schema mismatch errors (example: unknown column)
1. Stop services.
2. Run: `database/reset-and-init-databases.sql`
3. Start services again so JPA recreates schema.

## Gateway auth note
- API Gateway now validates JWT for protected routes.
- Use `Authorization: Bearer <token>` in protected API calls.

## Backend testing (JUnit + Mockito)
- Already enabled in all backend services via `spring-boot-starter-test` in each `pom.xml`.
- Use `JUnit` when:
  - You want to verify pure business logic and expected outputs/exceptions.
  - You are writing assertions for service/controller behavior.
- Use `Mockito` when:
  - Class under test depends on repository/client/external service.
  - You want to isolate unit test and avoid real DB/network calls.
  - You need to verify interactions (`save`, `delete`, etc.) or mock return values.
- Current unit test examples:
  - `auth-service`: `AuthServiceUnitTest`, `AuthServiceFollowUnitTest`
  - `comment-like-service`: `CommentServiceUnitTest`
  - `post-category-tag-service`: `PostServiceSavedPostUnitTest`
  - `newsletter-subscription-service`: `NewsletterServiceUnitTest`
  - `notification-service`: `NotificationServiceUnitTest`
- Run tests per service (from that service folder):
  - `./mvnw test` (or `mvn test` if Maven installed globally)

## Service responsibility mapping
- `auth-service`: users + roles (`READER`, `AUTHOR`, `ADMIN`)
- `post-category-tag-service`: posts + categories + tags
- `comment-like-service`: comments + comment likes
- `media-service`: local uploads + metadata
- `newsletter-subscription-service`: newsletter + subscriptions
- `notification-service`: notifications
- `api-gateway`: single entry point
- `eureka-server`: service registry

## Note
`mvnw` command wrapper is failing in this terminal sandbox (`Cannot start maven from wrapper`), so compile/runtime verification should be done in your local IDE terminal.

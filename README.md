# LinkChat Backend

Spring Boot 4.1 / Java 21 / PostgreSQL / Flyway / STOMP WebSocket backend.

## DDD layout
- `domain/model`: entities and domain enums
- `domain/repository`: repository ports
- `application`: use cases/services
- `infrastructure`: JPA adapters, WebSocket/security configuration, local file storage
- `interfaces`: REST and WebSocket adapters

## PostgreSQL
Create the database/user first (see root README), then set environment variables if you do not use defaults:
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `FRONTEND_URL`, `UPLOAD_DIR`.

Run: `mvn spring-boot:run`
Flyway automatically applies `src/main/resources/db/migration/V1__init.sql`.

Demo owner invite link code: `demo`.

## MVP security note
The API is intentionally open for local development. Before internet deployment add owner authentication/authorization and conversation-scoped visitor authorization. Browser tokens are hashed before database storage.

## Logging

The backend now uses SLF4J with Spring Boot logging. Each HTTP request receives an `X-Request-Id` correlation ID. The same ID is included in console log lines and structured API errors.

Default logging configuration:

- `INFO` for the application/root runtime
- `DEBUG` for `com.linkchat`
- request correlation ID in every HTTP-request log context
- business events logged without logging browser tokens or message bodies

Example:

```text
2026-08-13 10:15:22.123 INFO  [6b8d...] [http-nio-8080-exec-1] c.l.application.ChatApplicationService - Conversation created. conversationId=... ownerId=... visitorId=... inviteCode=demo
```

## Exception handling

REST exceptions are handled centrally by `ApiExceptionHandler`. Responses contain a stable error code, HTTP status, timestamp, path, request ID, message, and optional field-validation errors.

Examples of error codes:

- `RESOURCE_NOT_FOUND` -> HTTP 404
- `BUSINESS_RULE_VIOLATION` -> HTTP 400
- `VALIDATION_FAILED` -> HTTP 400
- `MALFORMED_REQUEST` -> HTTP 400
- `UPLOAD_TOO_LARGE` -> HTTP 413
- `STORAGE_ERROR` -> HTTP 500
- `INTERNAL_ERROR` -> HTTP 500

## Tests

Run all tests with:

```bash
mvn test
```

Unit tests cover application-service business rules, token hashing, and file storage. Integration tests use a real disposable PostgreSQL database through Testcontainers and verify Flyway + JPA + REST behavior, including the core rule that the same visitor clicking the same invite can create separate conversations.

Integration tests require Docker. They are configured with `disabledWithoutDocker = true`, so they are skipped when Docker is unavailable.

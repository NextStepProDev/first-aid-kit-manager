# CLAUDE.md

This file provides guidance to Claude Code when working with the backend API.

## Build & Run Commands

```bash
# Build project
./gradlew build

# Run locally (local profile has hardcoded credentials - no env vars needed)
./gradlew bootRun --args='--spring.profiles.active=local'

# Run with dev profile (requires docker postgres on port 5433)
./gradlew bootRun --args='--spring.profiles.active=dev'

# Run tests (uses test profile automatically)
./gradlew test

# Run a single test class
./gradlew test --tests "com.firstaidkit.unit.service.DrugServiceTest"

# Run a single test method
./gradlew test --tests "com.firstaidkit.unit.service.DrugServiceTest.shouldReturnDrug"

# Generate coverage report (outputs to build/reports/jacoco/test/html/)
./gradlew jacocoTestReport
```

### Docker Commands

```bash
# Full rebuild and start
docker-compose down -v --remove-orphans && ./gradlew build && docker-compose build --no-cache && docker-compose up

# Start postgres only (for local development)
docker-compose up -d postgres

# Start dev postgres (separate compose file, port 5433)
docker compose -f docker-compose-dev.yml up -d
```

## Architecture Overview

Spring Boot 4.2.0 application with Java 25, following a layered architecture with multi-tenant data isolation.

### Package Structure

```
com.firstaidkit/
├── controller/
│   ├── drug/           # DrugController - CRUD, search, export, delete-all
│   ├── auth/           # AuthController - login, register, tokens, profile, email verification, alerts
│   ├── admin/          # AdminController - user management, broadcast emails, email export
│   ├── web/            # PasswordResetViewController - web view for password reset
│   ├── dto/            # Request/response DTOs (admin/, auth/, drug/, error/)
│   └── handler/        # GlobalExceptionHandler - centralized error handling
├── service/            # Business logic: Admin, Auth, Drug, DrugForm, EmailVerification, PasswordReset, User
├── domain/exception/   # Custom exceptions (DrugNotFoundException, AccountLockedException, etc.)
└── infrastructure/
    ├── database/
    │   ├── entity/     # JPA entities (DrugEntity, UserEntity, RoleEntity)
    │   ├── repository/ # Spring Data JPA repositories
    │   └── mapper/     # MapStruct mappers (entity <-> DTO)
    ├── security/       # JWT auth, SecurityConfiguration, RateLimitingFilter, SecurityHeadersFilter
    ├── configuration/  # AsyncConfig, CacheConfig, OpenApiConfig
    ├── validation/     # Custom validators (@ValidPassword, @ValueOfEnum, etc.)
    ├── cache/          # UserAwareCacheKeyGenerator (multi-tenant cache keys)
    ├── email/          # EmailService (async SMTP with retry)
    ├── pdf/            # PdfExportService (OpenPDF library)
    ├── csv/            # CsvExportService (semicolon-separated for Polish Excel)
    ├── bootstrap/      # BootstrapApplicationComponent, DataInitializer, FlywayInitializer
    └── util/           # DateUtils
```

### Key Architectural Patterns

**Multi-Tenancy**: Each user's data is isolated via `user_id` foreign key. `CurrentUserService` extracts the authenticated user from security context. All repository queries filter by user ID. `UserAwareCacheKeyGenerator` ensures cache keys include user ID.

**JWT Authentication**: Access token (1h) + refresh token (24h) with rotation. `JwtTokenProvider` handles token generation/validation. `JwtAuthenticationFilter` validates tokens on each request. Stateless sessions.

**Email Verification**: New users must verify email before login. Token-based verification via `EmailVerificationTokenEntity`. Resend verification endpoint available.

**Caching**: Caffeine-backed Spring Cache with caches: `drugById`, `drugsSearch`, `drugStatistics`. All mutating operations use `@CacheEvict`. Cache keys are user-aware for tenant isolation.

**Database Migrations**: Flyway migrations in `src/main/resources/db/migration/`. Schema versioned V1.0 through V11.0.

**Async Processing**: `@EnableAsync` with dedicated `emailTaskExecutor` thread pool. Email sending is async with Resilience4j retry (3 attempts, exponential backoff).

## API Endpoints

### Auth Endpoints
- `POST /api/auth/register` - Register new user (triggers verification email)
- `POST /api/auth/login` - Login (returns JWT tokens)
- `POST /api/auth/refresh` - Refresh tokens (with rotation)
- `GET /api/auth/me` - Get current user profile
- `PUT /api/auth/profile` - Update user profile (name, username)
- `PUT /api/auth/alerts` - Toggle alert notifications
- `POST /api/auth/forgot-password` - Initiate password reset
- `POST /api/auth/reset-password` - Complete password reset
- `POST /api/auth/change-password` - Change password
- `DELETE /api/auth/account` - Delete account
- `GET /api/auth/verify-email?token=...` - Verify email address
- `POST /api/auth/resend-verification` - Resend verification email

### Drug Endpoints
- `GET /api/drugs/search?name=&form=&expired=&page=&size=&sort=` - Search and filter drugs
- `GET /api/drugs/{id}` - Get single drug
- `POST /api/drugs` - Create new drug
- `PUT /api/drugs/{id}` - Update drug
- `DELETE /api/drugs/{id}` - Delete drug
- `GET /api/drugs/forms` - Get available drug forms
- `GET /api/drugs/forms/dictionary` - Get drug forms dictionary
- `GET /api/drugs/statistics` - Get dashboard statistics
- `GET /api/drugs/export/pdf` - Export drugs to PDF
- `GET /api/drugs/export/csv` - Export drugs to CSV
- `POST /api/drugs/delete-all` - Delete all user's drugs (password protected)

### Admin Endpoints (requires ADMIN role)
- `GET /api/admin/users?page=&size=&sort=` - List all users (paginated)
- `DELETE /api/admin/users/{userId}` - Delete user account
- `POST /api/admin/broadcast` - Send broadcast email to all users
- `POST /api/admin/users/{userId}/email` - Send email to specific user
- `GET /api/admin/emails/csv` - Export all user emails as CSV

### Health & Monitoring
- `GET /actuator/health` - Health check (public)
- `GET /actuator/info` - App info (public)
- `GET /actuator/metrics` - Metrics (requires ADMIN role)

## Security Features

### Rate Limiting
- Bucket4j-based filter on auth endpoints (`/api/auth/login`, `/register`, `/forgot-password`, `/reset-password`)
- Default: 20 requests/minute per IP
- Config: `app.rate-limit.enabled`, `app.rate-limit.requests-per-minute`

### Security Headers (Production)
- SecurityHeadersFilter adds security headers in docker/prod profiles

### Account Lockout
- 5 failed login attempts -> 15 minute lockout
- Tracked in `UserEntity.failedLoginAttempts` and `lockedUntil`
- Auto-reset on successful login

### Password Complexity
- `@ValidPassword` annotation requires: min 8 chars, uppercase, lowercase, digit, special char

### CORS
- Config: `app.cors.allowed-origins` (default: `http://localhost:3000,http://localhost:5173`)

## Testing Structure

- **Integration tests** (`src/test/java/.../integration/`): Testcontainers with PostgreSQL. Extend `AbstractIntegrationTest`.
- **Slice tests** (`src/test/java/.../slice/controller/`): Controller-layer validation without DB.
- **Unit tests** (`src/test/java/.../unit/service/`): Business logic with mocked dependencies.
- **E2E tests** (`src/test/java/.../integration/e2e/`): Full workflow tests.
- **Test config** in `src/test/java/.../config/`.

JaCoCo line coverage gate: 53% (`jacocoTestCoverageVerification`, run in CI before the image build). Raise it as tests are added.

## Configuration Profiles

| Profile | Port | Use Case |
|---------|------|----------|
| `local` | 8082 | IDE development, hardcoded credentials |
| `dev` | 8082 | Local with Docker postgres (port 5433) |
| `docker` | 8081 | Full Docker Compose deployment, Swagger disabled |
| `prod` | 8080 | Production, Swagger disabled, stricter rate limits (10/min) |
| `test` | - | Automated tests (auto-activated) |

Environment variables needed for non-local profiles: `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS` (optional).

## Key Configuration Properties

```yaml
app:
  jwt:
    secret: ${JWT_SECRET}
    access-token-expiration-ms: 3600000    # 1 hour
    refresh-token-expiration-ms: 86400000  # 24 hours
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173}
  rate-limit:
    enabled: true
    requests-per-minute: 20
  admin:
    email: ${ADMIN_EMAIL}  # Auto-grants ADMIN role on registration
```

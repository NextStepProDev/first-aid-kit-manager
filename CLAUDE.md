# CLAUDE.md

First Aid Kit Manager - aplikacja do zarządzania domową apteczką. Monorepo z backendem (Java/Spring Boot) i frontendem (React/TypeScript).

## Project Structure

```
first-aid-kit-manager/
├── first-aid-kit-api/           # Backend - Spring Boot 4.2.0, Java 25
├── first_aid_kit_frontend/      # Frontend - React 19, TypeScript 6, Vite 8, Tailwind CSS 4
└── first-aid-kit-manager-hub/   # Deployment hub - docker-compose for full stack
```

Each subdirectory has its own `CLAUDE.md` with detailed documentation.

## Repo i CI (monorepo od 08.11.2026)

Jedno repo git w tym folderze: `NextStepProDev/first-aid-kit-manager` (dawne repo huba po
zmianie nazwy; sekrety `DEPLOY_*` zostały w nim). Wcześniej były trzy osobne repo
(`first-aid-kit-api`, `first-aid-kit-web`, `first-aid-kit-manager-hub`), ich historia jest
zachowana w podfolderach, tagi backendu mają prefiks `api/`. Stare repo api i web są
archiwalne. Commit i push zawsze z roota.

Workflowy w `/.github/workflows/`:
- `ci-backend.yml`: testy + JaCoCo, potem obraz `ghcr.io/nextstepprodev/first-aid-kit-api`;
  odpala się tylko na zmiany w `first-aid-kit-api/**`
- `ci-frontend.yml`: lint, `tsc -b`, testy, potem obraz `ghcr.io/nextstepprodev/first-aid-kit-web`
  (tylko z `main`); tylko na zmiany w `first_aid_kit_frontend/**`
- `deploy.yml`: ręczny deploy na VM (workflow_dispatch)
- `dependency-submission.yml`: wysyła do GitHuba rozwiązane zależności backendu. ⚠️ To jedyne
  źródło alertów bezpieczeństwa dla Gradle — graf zależności GitHuba nie czyta `build.gradle` ani
  `libs.versions.toml`. Bez niego graf pokazywał 0 paczek Maven i żaden alert o Springu nie
  przychodził.

Nazwy obrazów GHCR celowo bez zmian: compose na VM ich używa.

**Dependabot** (`.github/dependabot.yml`, od 27.11.2026): co poniedziałek 06:00 czasu
warszawskiego, jak we wszystkich projektach. Wersje backendu siedzą w
`first-aid-kit-api/gradle/libs.versions.toml` — **nie wracać do `"${...}"` z `gradle.properties`**.
Compose z huba nie jest tym, co chodzi na VM (deploy go nie wysyła), więc PR-y compose wyrównują
dev z produkcją, a nie zmieniają produkcji.

Otwarte zadania dla całego stacku: `TODO.md`.

## Quick Start (Local Development)

```bash
# 1. Start dev database (postgres on port 5433)
cd first-aid-kit-api && docker compose -f docker-compose-dev.yml up -d

# 2. Start backend (port 8082)
cd first-aid-kit-api && ./gradlew bootRun --args='--spring.profiles.active=dev'

# 3. Start frontend (Vite dev server on port 5173, backend on 8082)
cd first_aid_kit_frontend && npm run dev
```

## Architecture Overview

**Backend** (`first-aid-kit-api/`): Spring Boot REST API. Layered architecture: controller -> service -> repository. Multi-tenant (user data isolated via user_id). JWT auth (access + refresh tokens). PostgreSQL with Flyway migrations (V1.0-V13.0). Caffeine caching. Async email with Resilience4j retry.

**Frontend** (`first_aid_kit_frontend/`): React SPA. React Query for server state. React Hook Form + Zod for forms. Axios with JWT interceptors and automatic token refresh. Tailwind CSS dark theme. Polish locale throughout.

**Communication**: Frontend -> Backend via REST API. Base URL configured via `VITE_API_URL` env var (default: `http://localhost:8082`). All API calls go through `/api/*`. JWT Bearer token in Authorization header.

## Key Features

- Drug CRUD with search, filtering (by form, expiration status), sorting, pagination
- CSV import/export and PDF export of drugs
- Dashboard with statistics (total, active, expired, alerts)
- User registration with email verification
- Password reset via email
- User profile management with alert preferences
- Admin panel (user management, broadcast emails)
- Account lockout after failed login attempts
- Rate limiting on auth endpoints

## Common Workflows

**Full-stack feature**: Typically involves adding endpoint in backend controller + service + repository, then calling it from frontend via api/ module, and updating the relevant page/component.

**Backend only**: `cd first-aid-kit-api && ./gradlew test` to run tests. JaCoCo line coverage gate: 53% (run in CI before the image build).

**Frontend only**: `cd first_aid_kit_frontend && npm start` for dev, `npm test` for tests.

**Docker full stack**: Use `first-aid-kit-manager-hub/docker-compose.yml` or `first-aid-kit-api/docker-compose.yml`.

## API Connection Map

| Frontend Page | Backend Endpoints |
|--------------|-------------------|
| DashboardPage | `GET /api/drugs/statistics`, `GET /api/drugs/search` |
| DrugsPage | `GET /api/drugs/search`, `DELETE /api/drugs/{id}`, exports |
| DrugFormPage | `POST /api/drugs`, `PUT /api/drugs/{id}`, `GET /api/drugs/{id}` |
| ProfilePage | `GET /api/auth/me`, `PUT /api/auth/profile`, `PUT /api/auth/alerts`, `POST /api/auth/change-password`, `DELETE /api/auth/account` |
| LoginPage | `POST /api/auth/login` |
| RegisterPage | `POST /api/auth/register` |
| AdminUsersPage | `GET /api/admin/users`, `DELETE /api/admin/users/{id}`, broadcast |

## Tech Stack Summary

| Layer | Technology |
|-------|-----------|
| Backend | Java 25, Spring Boot 4.2.0, Spring Security, Spring Data JPA |
| Database | PostgreSQL 18.7, Flyway migrations |
| Frontend | React 19, TypeScript 6, React Router 7, React Query 5, Vite 8 |
| Styling | Tailwind CSS 4, dark theme, i18n (PL/EN) |
| Forms | React Hook Form 7, Zod 4 |
| Auth | JWT (access 1h + refresh 24h), Axios interceptors |
| Build | Gradle 9.9 (backend), Vite/npm on Node 26 (frontend), Docker multi-stage |

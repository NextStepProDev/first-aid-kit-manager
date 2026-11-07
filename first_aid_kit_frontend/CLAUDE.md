# CLAUDE.md

This file provides guidance to Claude Code when working with the frontend.

## Build & Run Commands

```bash
npm run dev        # Vite dev server on localhost:5173 (HMR)
npm run build      # tsc -b + production build -> dist/
npm test           # Vitest (single run); npm run test:watch for watch mode. CI (PR + main) runs lint, tsc -b and tests before the image build
npm run lint       # ESLint
```

## Tech Stack

- **React 19** with TypeScript 6
- **React Router 7** - Client-side routing
- **TanStack React Query 5** - Server state management, caching, query invalidation
- **React Hook Form 7 + Zod 4** - Form handling and schema validation
- **Axios** - HTTP client with JWT interceptors
- **Tailwind CSS 4** (via `@tailwindcss/vite`) - Utility-first styling, dark theme (class-based), custom color palette
- **lucide-react** - Icon library
- **react-hot-toast** - Toast notifications
- **Vite 8 + Vitest 5** - Build toolchain and test runner (Node 26)

## Project Structure

```
src/
├── api/                    # API integration layer
│   ├── client.ts           # Axios instance, JWT interceptors, token refresh logic
│   ├── auth.ts             # Auth endpoints (login, register, verify, profile, alerts)
│   ├── drugs.ts            # Drug CRUD, search, export, statistics
│   └── admin.ts            # Admin operations (users, emails, broadcast)
├── components/
│   ├── ui/                 # Reusable UI components (Button, Input, Card, Modal, Badge, Spinner, Pagination, Select, Textarea, ConfirmDialog, EmptyState)
│   ├── features/
│   │   ├── auth/           # Auth-specific components
│   │   └── drugs/          # Drug-specific components
│   ├── layout/             # Layout, Navbar, Sidebar, Footer, AuthLayout
│   ├── ProtectedRoute.tsx  # Auth route guards (ProtectedRoute + PublicRoute)
│   ├── ErrorBoundary.tsx   # React error boundary
│   ├── ImportCsvModal.tsx  # CSV import for drugs
│   └── DeleteAllDrugsModal.tsx
├── contexts/
│   └── AuthContext.tsx      # Global auth state (user, login, logout, refreshUser)
├── hooks/                   # Custom hooks (directory ready for expansion)
├── pages/                   # Route page components
│   ├── DashboardPage.tsx    # Statistics, recent drugs, charts
│   ├── DrugsPage.tsx        # Drug list with search, filter, sort, pagination, export
│   ├── DrugFormPage.tsx     # Create/edit drug form
│   ├── ProfilePage.tsx      # User profile, change password, alerts, delete account
│   ├── AdminUsersPage.tsx   # Admin user management
│   ├── LoginPage.tsx, RegisterPage.tsx, ForgotPasswordPage.tsx, ResetPasswordPage.tsx, VerifyEmailPage.tsx
│   ├── ContactPage.tsx, AboutUsPage.tsx, NotFoundPage.tsx
│   └── index.ts
├── types/
│   └── index.ts             # TypeScript interfaces (User, Drug, Auth, Admin types)
├── utils/
│   ├── cn.ts                # Tailwind class merger (clsx + tailwind-merge)
│   ├── formatDate.ts        # Date formatting, expiration checks (Polish locale)
│   └── csvUtils.ts          # CSV parsing for drug import (multiple date formats)
├── config/
│   └── version.ts           # App version string
├── App.tsx                  # Root component with routing setup and Toaster
└── index.tsx                # Entry point
```

## Key Patterns

**API Client** (`api/client.ts`): Axios instance with base URL from `VITE_API_URL` (empty = same-origin). Request interceptor adds JWT Bearer token from localStorage. Response interceptor handles 401 by refreshing token and queuing failed requests.

**Auth Flow**: AuthContext provides `user`, `isAuthenticated`, `login()`, `logout()`, `refreshUser()`. Tokens stored in localStorage (`accessToken`, `refreshToken`). ProtectedRoute redirects to /login if unauthenticated. PublicRoute redirects to / if already authenticated.

**Data Fetching**: React Query for all server state. Query keys follow pattern like `['drugs', 'search', params]`. Cache invalidation on mutations via `queryClient.invalidateQueries()`.

**Forms**: React Hook Form + Zod schemas. Validation schemas defined inline or in component files.

**UI Components**: Variant-based system (primary/secondary/danger/ghost). Size system (sm/md/lg). All use `cn()` utility for class composition.

**Routing**: React Router 7. Public routes (login, register, forgot-password, reset-password, verify-email). Protected routes (dashboard, drugs, profile, admin). Layout wraps protected routes with Navbar + Sidebar.

## Environment Variables

```
VITE_API_URL=http://localhost:8082   # Backend API base URL
```

## Localization

All UI text is in Polish (pl-PL). Date formatting uses Polish locale. No i18n library - strings are hardcoded in components.

## Docker

Multi-stage build: Node 26 Alpine (build) -> Nginx Alpine (serve). Nginx config includes reverse proxy to backend. Build arg: `VITE_API_URL`.

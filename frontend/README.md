# Service Business Platform — Frontend

React + TypeScript SPA for the Service Business Platform (salons, clinics, gyms,
rental businesses). This covers Phases 1–3 of `CLAUDE_CODE.md`: public discovery,
auth, customer booking, and the business owner dashboard (profile, services,
staff, hours, bookings). It is built strictly against
[`docs/API_CONTRACT.md`](../docs/API_CONTRACT.md) — field names, enums, the
pagination envelope, the error shape, and the booking state machine all mirror
that document.

## Stack

Vite, React 18, TypeScript (`strict`), React Router v6, TanStack Query
(`@tanstack/react-query`), React Hook Form + Zod, Tailwind CSS, Vitest +
React Testing Library.

## Getting started

```bash
npm install
cp .env.example .env   # adjust VITE_API_BASE_URL if your backend runs elsewhere
npm run dev            # http://localhost:5173
```

The backend is expected at `http://localhost:8080/api/v1` (see `docker-compose.yml`
at the repo root for the Postgres dependency the backend needs). Without a
backend running, the app still loads — every data screen has a real loading
state, then falls back to its error state once the request fails, rather than
hanging or rendering blank.

### Environment variables

| Variable              | Default                          | Purpose                                   |
|------------------------|-----------------------------------|--------------------------------------------|
| `VITE_API_BASE_URL`   | `http://localhost:8080/api/v1`   | Base URL prefixed onto every API request. |

### Other scripts

```bash
npm run build     # tsc -b && vite build
npm run preview   # preview the production build
npm run lint      # oxlint
npm test          # vitest run
npm run test:watch
```

## Architecture

```
src/
├── app/          App shell: providers (react-query, auth, toasts), router mount, Navbar/RootLayout
├── routes/       Route wiring + guards (RequireAuth, RequireBusinessAccess) + small standalone pages
├── components/
│   ├── ui/       Dumb, reusable primitives (Button, Input, Select, Card, Modal, Badge, Table,
│   │             Pagination, EmptyState, ErrorState, LoadingSpinner/Skeleton, Toast, FormError) —
│   │             no API calls, no domain logic
│   ├── business/ Presentational business pieces (BusinessHeader, BusinessCard, ServiceCard,
│   │             StaffCard, HoursTable, BusinessStatusBadge)
│   └── booking/  Presentational booking pieces (SlotPicker, BookingCard, BookingStatusBadge)
├── features/
│   ├── auth/         AuthContext/useAuth, LoginForm, RegisterForm
│   ├── discovery/    BusinessSearchPage, BusinessProfilePage
│   ├── booking/      BookingFlow (service → staff → slot → confirm), MyBookingsPage
│   ├── business/     Dashboard: CreateBusinessForm, DashboardHome/Layout, BusinessProfileEditor,
│   │                 ServicesManager, StaffManager, HoursEditor (+ their form components/schemas)
│   └── dashboard/    BookingsManager (business-side booking lifecycle actions)
├── services/api/  One thin typed module per backend feature (authApi, userApi, businessApi,
│                  catalogApi, staffApi, availabilityApi, bookingApi, discoveryApi) — all HTTP
│                  details (base URL, auth header, 401-refresh-retry, error mapping) live in
│                  the single shared apiClient
├── hooks/         queryKeys (central react-query key registry), useToast, useDebouncedValue,
│                  useEntityLookup
├── types/         TypeScript interfaces mirroring every DTO/enum in API_CONTRACT.md byte-for-byte
├── utils/         apiError (error-code → message mapping), bookingTransitions (the booking state
│                  machine — the only place allowed to decide which lifecycle action is legal),
│                  money/date formatting, cn, categoryLabels
└── styles/        Tailwind entrypoint
```

No component makes a raw `fetch` call, and no component computes booking
transitions or price formatting itself — that all lives in `services/api` /
`utils`, per the project's frontend rules.

## Auth flow

- The access token lives only in memory (module state inside `apiClient`,
  mirrored in `AuthContext`) — never in `localStorage`.
- The refresh token is the backend's httpOnly `refreshToken` cookie. On app
  boot, `AuthProvider` calls `POST /auth/refresh` once to silently restore a
  session (or confirm there isn't one).
- On any `401` from an authenticated request, `apiClient` transparently
  attempts one `POST /auth/refresh` and retries the original request before
  giving up and logging the user out. Auth endpoints themselves
  (login/register/refresh/logout) skip this dance, since a `401` there means
  "invalid credentials", not "expired token".
- Route guards (`routes/RequireAuth.tsx`, `routes/RequireBusinessAccess.tsx`)
  are UX-only, per `CLAUDE_CODE.md` §30 — the backend is the real
  authorization boundary.

## Booking state machine

`utils/bookingTransitions.ts` is the single source of truth for which of the
seven explicit lifecycle endpoints (`confirm`, `check-in`, `start`,
`complete`, `cancel`, `no-show`, `reject`) are legal for a given
`BookingStatus`. The business dashboard's `BookingsManager` only ever renders
buttons for actions this module returns — there is no code path that PATCHes
an arbitrary `status` value. The backend re-validates every transition
regardless; this only prevents the UI from offering an action the API would
reject with `INVALID_STATE_TRANSITION`.

## Testing

```bash
npm test
```

17 tests across 6 files, covering:

- **Booking flow** (`__tests__/BookingFlow.test.tsx`) — slot selection (available
  vs. disabled slots) and the confirm step, including the exact payload sent to
  `POST /businesses/{id}/bookings`.
- **Login/Register validation** (`__tests__/LoginForm.test.tsx`,
  `__tests__/RegisterForm.test.tsx`) — required fields, email format, minimum
  password length, and surfacing a mocked backend error (both a `401
  UNAUTHENTICATED` on login and a `400 VALIDATION_ERROR` on register) instead
  of a generic fallback message.
- **API error state** (`__tests__/MyBookingsPage.test.tsx`) — a failed query
  renders the shared `ErrorState` (not a blank screen), a "Try again" retry
  works, and a successful-but-empty response renders the shared `EmptyState`.
- **Booking transitions** (`__tests__/bookingTransitions.test.ts`) — the state
  machine helper itself.
- **App wiring** (`__tests__/App.test.tsx`) — providers/router mount and the
  landing page renders end to end.

## Known deviations from / assumptions beyond the contract

`API_CONTRACT.md` is otherwise followed exactly (field names, enums, pagination
envelope, error shape). A few gaps in the contract required a judgment call:

1. **`CreateBusinessRequest` / `UpdateBusinessRequest` shapes are inferred.**
   The contract documents `BusinessDto`'s response shape but not the
   create/update request bodies for `POST /businesses` and
   `PATCH /businesses/{id}`. This UI sends
   `{ name, category, description, phone, email }` on create and
   `{ name, description, phone, email, logoUrl, coverImageUrl }` on update
   (category is treated as fixed after creation; capabilities aren't editable
   from this UI yet since no Phase 4+ feature depends on them here).
2. **Business creation isn't an explicitly-scoped screen, but is included.**
   The dashboard is unreachable without an existing business, and the contract
   provides `POST /businesses` for exactly this, so a minimal "create your
   business" form is shown at `/dashboard` for an owner with zero memberships.
3. **`BookingDto` carries only `businessId`/`serviceId`/`staffId`, no names.**
   For the business dashboard this is a non-issue — `BookingsManager` already
   loads that business's own services/staff (owner-scoped, so it's an
   authoritative join). For the customer's cross-business "My Bookings" list,
   there is no documented endpoint a customer can call to resolve an arbitrary
   business/service name from an id. `hooks/useEntityLookup.ts` opportunistically
   reads names from whatever discovery data react-query already has cached in
   the session (e.g. the customer browsed that business's public profile
   earlier) and falls back to a shortened id when nothing is cached — this adds
   no new network calls or invented API surface. Consider adding a summary
   (business/service name) to `BookingDto`, or a batch lookup endpoint, in a
   future contract revision.
4. **React 18, not 19.** `npm create vite@latest` currently scaffolds React 19;
   pinned back to React 18 per the tech-stack decision.

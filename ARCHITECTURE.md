# ARCHITECTURE.md — Service Business Platform Backend

This document describes the architecture actually implemented for Phases 1-5
(Foundation, Services, Appointments, Queue, Memberships) of `CLAUDE_CODE.md`,
against the interface frozen in `docs/API_CONTRACT.md`. Where a decision
wasn't fully specified by either document, the choice made and its rationale
is recorded here.

## 1. The capability model

The platform is one shared codebase for salons, clinics, gyms, and rental
businesses — not a separate application per category (`CLAUDE_CODE.md` §3,
§53). Two orthogonal concepts express this:

```
BusinessCategory    — what a business IS      (SALON, GYM, CAR_RENTAL, ...)
BusinessCapability   — what a business CAN DO   (APPOINTMENTS, MEMBERSHIPS, ...)
```

A `Business` has exactly one category and a *set* of capabilities. Application
code branches on `business.capabilities.contains(APPOINTMENTS)`, never on
`business.category == SALON`. Both enums are defined with their **full** value
set from `CLAUDE_CODE.md` §7-8 — Phases 1-3 only implemented working logic for
`APPOINTMENTS`/`STAFF`; this pass adds working logic for `QUEUE` and
`MEMBERSHIPS` the same way, gating `POST .../queue` and
`POST .../memberships` on the respective capability being enabled
(`ValidationException` → 400 otherwise). `RENTALS`/`CLASSES`/`CAPACITY`/
`RESOURCES`/`PAYMENTS` remain reserved for later phases.

The end-to-end model this phase implements is the full core abstraction
(`CLAUDE_CODE.md` §54):

```
BUSINESS → CAPABILITIES → SERVICES / STAFF → AVAILABILITY → BOOKING / QUEUE / MEMBERSHIP
```

Concretely, per business: `Service` (the bookable item), `StaffMember`
(optionally assigned to services via `StaffServiceAssignment`), `BusinessHours`
(per day-of-week open/close intervals), `Booking` (the appointment
reservation), `QueueEntry` (the walk-in queue position), and
`MembershipPlan`/`Membership` (purchasable recurring access) — the last three
each governed by their own explicit state machine.

## 2. Package structure

Feature-oriented packages under `com.platform`, each with
`controller` / `service` / `repository` / `domain` / `dto` sub-packages as
applicable:

```
auth        — register/login/refresh/logout, opaque revocable refresh tokens
user        — the platform User + /users/me endpoints
business    — Business, BusinessLocation, BusinessHours, BusinessMembership,
              BusinessCategory/BusinessCapability enums, discovery (public
              search/profile) — discovery lives here because it is entirely
              about the business aggregate, and CLAUDE_CODE.md §27 does not
              list a separate "discovery" package
catalog     — the bookable-item entity, named "Service" per the domain spec;
              package name deliberately avoids colliding with
              org.springframework.stereotype.Service
staff       — StaffMember + the staff<->service assignment join
availability— pure slot-generation math + the orchestration service that
              loads business hours/service duration/staff/bookings
booking     — the Booking aggregate, its explicit state machine, and both the
              business-management and customer-facing (/me/bookings)
              controllers
queue       — the QueueEntry aggregate, QueueStateMachine, and both the
              business-management and customer-facing (/me/queue-entries)
              controllers; position/estimatedWaitMinutes are computed in
              QueueService, never stored
membership  — MembershipPlan + Membership aggregates, MembershipStateMachine,
              plan management + purchase/freeze/reactivate/cancel controllers,
              and the customer-facing /me/memberships controller
customer    — CustomerProfile (platform-wide identity) + BusinessCustomer
              (per-business relationship), auto-created on first booking
common      — exception (ApiException hierarchy + GlobalExceptionHandler),
              security (JWT filter/service, CurrentUser, BusinessAccessService
              lives in `business` since it needs BusinessMembershipRepository),
              pagination (PageResponse<T>), response (ErrorResponse)
config      — SecurityConfig (JWT filter chain, CORS, PasswordEncoder)
```

Controllers only ever accept/return DTOs; JPA entities never cross the API
boundary (`CLAUDE_CODE.md` §26/§46).

### Deliberate simplification: plain UUID foreign keys, not JPA object graphs

Child entities (`BusinessLocation`, `BusinessHours`, `Service`, `StaffMember`,
`Booking`, `QueueEntry`, `MembershipPlan`, `Membership`, `BusinessMembership`,
`CustomerProfile`, `BusinessCustomer`) hold
their parent's id as a plain `UUID` column — there are no `@ManyToOne` /
`@OneToMany` JPA associations across aggregates. This is a conscious trade-off
for `CLAUDE_CODE.md` §53's "simple + explicit + testable" over "generic +
abstract": every cross-aggregate lookup is an explicit repository call with an
explicit tenant-scoping parameter (`findByIdAndBusinessId`, `findByBusinessId`,
...), which makes it structurally hard to accidentally leak another tenant's
data through a lazy-loaded relation, and avoids `open-in-view` / N+1 surprises
entirely (Hibernate's `open-in-view` is disabled).

## 3. Multi-tenant authorization

Every business is a tenant boundary (`CLAUDE_CODE.md` §31). The chain for any
`/businesses/{businessId}/**` management endpoint is:

```
authenticated user (JWT) → BusinessMembership lookup for this exact businessId → operation
```

This is implemented once, as a reusable `@PreAuthorize` SpEL expression backed
by a single Spring bean:

```java
@PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'OWNER')")
```

`BusinessAccessService.hasRole(businessId, authentication, requiredRole)`:
- returns `true` unconditionally for platform `ADMIN` (authority `ROLE_ADMIN`);
- otherwise loads the caller's `BusinessMembership` row for that exact
  `businessId` (never trusting the path variable beyond using it as a lookup
  key) and checks the role: `"OWNER"` requires an OWNER membership exactly;
  `"STAFF"` accepts either STAFF or OWNER (i.e. "at least staff-level access").

Every controller method that touches business-scoped data carries this
annotation — there is no hand-rolled `if (!isOwner(...))` check duplicated
across controllers. `CrossTenantAuthorizationIntegrationTest` proves a second
business owner is rejected (403) from reading/writing a business they don't
belong to, and that an anonymous request to any protected endpoint gets 401.

Customer-facing `/me/**` endpoints use a different, narrower rule: the caller
can only ever act on data tied to *their own* `CustomerProfile` (resolved from
the JWT's subject, never accepted as a client-supplied id) — see
`BookingService.cancelByCustomer`.

## 4. Booking state machine

`BookingStateMachine` (in `booking.domain`) is a static, explicit transition
table — not stringly-typed status juggling:

```
PENDING     → CONFIRMED, REJECTED, CANCELLED
CONFIRMED   → CHECKED_IN, CANCELLED, NO_SHOW
CHECKED_IN  → IN_PROGRESS, CANCELLED
IN_PROGRESS → COMPLETED
```

Every other transition throws `InvalidStateTransitionException` → HTTP 409
`INVALID_STATE_TRANSITION`. There is no generic "update booking status"
endpoint; each lifecycle action (`/confirm`, `/check-in`, `/start`,
`/complete`, `/cancel`, `/no-show`, `/reject`) maps to exactly one target
status. `Booking.transitionTo()` is the only way to change a booking's status,
and it always goes through the state machine.

## 5. Booking creation: conflict-safety under concurrency

`endAt` and `price`/`currency` are always computed server-side from the
`Service` entity — `CreateBookingRequest` only carries `serviceId`, `staffId`,
`startAt`, `notes`. Before inserting, `BookingService.create()`:

1. Validates the business is `ACTIVE`, the service is `ACTIVE` and of type
   `APPOINTMENT` with a valid duration.
2. Computes `endAt = startAt + service.durationMinutes`.
3. Validates `[startAt, endAt)` is fully contained in a `BusinessHours`
   interval for that day-of-week.
4. Takes a **Postgres transaction-scoped advisory lock**
   (`pg_advisory_xact_lock`) keyed on `(businessId, staffId)`.
5. Re-checks for an overlapping non-terminal booking for that staff (or the
   shared calendar, if no staff is assigned) — see `TimeRangeUtil.overlaps`.
6. Inserts the booking and commits, releasing the lock.

The advisory lock exists because there is no existing row to `SELECT ... FOR
UPDATE` before the *first* booking in a slot is created — a plain
"check-then-insert" has a race window where two concurrent requests for the
same slot could both pass the overlap check. The lock serializes concurrent
attempts for the same (business, staff) pair at the database level: the second
request to acquire the lock only proceeds after the first has committed (or
rolled back), so it always sees the first booking's row when it re-runs the
overlap check.

## 6. Availability calculation

`AvailabilityService` (orchestration) + `SlotCalculator` (pure, unit-tested
math) implement `CLAUDE_CODE.md` §13/§36. For a given business + service +
date (+ optional staff):

1. Determine the "tracks" to generate slots for: the single requested staff
   member if `staffId` was given, otherwise every `ACTIVE` staff member
   assigned to the service, or — if no staff is assigned/required for that
   service — a single shared business-wide calendar (`staffId = null`).
2. For each track, generate back-to-back slots of length
   `service.durationMinutes` within each `BusinessHours` interval for that
   day-of-week.
3. Mark a slot unavailable if it starts in the past, or overlaps an existing
   non-terminal booking for that track.

**Timezone simplification**: business hours are treated as UTC in this phase —
there is no per-business timezone field yet. All `startAt`/`endAt` values are
UTC `Instant`s per `API_CONTRACT.md`. Adding a business timezone is a
straightforward follow-up (a column on `Business` + converting local
hours↔UTC in `SlotCalculator`) but was out of scope for Phases 1-3.

## 7. Auth

- Access tokens are short-lived (15 min) signed JWTs (`io.jsonwebtoken`,
  HS256), carrying the user id, email, and roles as claims. Stateless —
  validated per-request by `JwtAuthenticationFilter`, no server-side session.
- Refresh tokens are **not** JWTs: they are opaque random strings, stored
  server-side as a SHA-256 hash with an expiry and revocation flag. This is
  what makes `logout` and refresh-token rotation actually able to invalidate a
  token — a stolen-but-unexpired JWT refresh token could not be revoked before
  its own expiry, an opaque server-tracked one can be revoked immediately. The
  refresh token is rotated (old one revoked, new one issued) on every use.
- The refresh token travels only as an `httpOnly; Secure; SameSite=Strict`
  cookie named `refreshToken`, scoped to `/api/v1/auth/refresh`, per the
  contract.
- Passwords are hashed with BCrypt (Spring Security's `PasswordEncoder`).

## 8. Queue

`QueueService` (`queue.service`) implements `CLAUDE_CODE.md` §15-16/§35.

- **Position is never stored.** `QueueEntry` has no `position` column at all —
  `QueueService` derives the 1-based rank on every read by ordering the
  business's currently `WAITING`/`CALLED` entries by `joined_at` ascending
  (`QueueEntryRepository.findByBusinessIdAndStatusInOrderByJoinedAtAsc`).
  `SERVING`/terminal entries report position `0` (they're no longer "in line").
- **`estimatedWaitMinutes` heuristic** (documented assumption, since
  `CLAUDE_CODE.md` only asks for "a simple heuristic"): `peopleAhead ×
  thisEntry'sServiceDurationMinutes`, defaulting to 15 minutes when the
  service has no configured duration. This is deliberately simple — it does
  not track a running average of actually-completed service times, which
  would need its own aggregate and was judged out of scope for an MVP "roughly
  how long" figure.
- **Duplicate-active-entry prevention** (§35): before inserting, `join()`
  takes a Postgres transaction-scoped advisory lock keyed on
  `(businessId, customerProfileId)` — same pattern as
  `BookingService.acquireSlotLock` — then checks
  `existsByBusinessIdAndCustomerIdAndStatusIn(WAITING, CALLED, SERVING)`. The
  lock closes the same race window described in §5 for bookings: without it,
  two concurrent join requests from the same customer could both pass the
  existence check before either commits.
- **`QueueStateMachine`** (`queue.domain`), same static-transition-table shape
  as `BookingStateMachine`:
  ```
  WAITING → CALLED, SKIPPED, CANCELLED
  CALLED  → SERVING, NO_SHOW, CANCELLED
  SERVING → COMPLETED
  ```
  Any other transition → 409 `INVALID_STATE_TRANSITION`, via
  `QueueEntry.transitionTo()` (the only mutator of `status`).
- Joining requires the business to have the `QUEUE` capability enabled and the
  target service to be `ACTIVE` and of `bookingType` `QUEUE` or `WALK_IN`
  (400 `VALIDATION_ERROR` otherwise) — checked in that order in
  `QueueService.join()`.
- A new error code, **`QUEUE_CONFLICT`** (409), was added
  (`QueueConflictException`) for the duplicate-active-entry case — it isn't
  one of `API_CONTRACT.md`'s pre-existing standard codes, so it follows the
  same `ApiException` → `GlobalExceptionHandler` pattern as every other code
  rather than overloading `BOOKING_CONFLICT` or `VALIDATION_ERROR` for a
  semantically distinct situation.

## 9. Memberships

`MembershipService`/`MembershipPlanService` (`membership.service`) implement
`CLAUDE_CODE.md` §17.

- **No payment gate this phase** (`CLAUDE_CODE.md` §22): `purchase()` creates
  the `Membership` as `ACTIVE` immediately once the business has the
  `MEMBERSHIPS` capability and the target plan is `ACTIVE`; `paymentId` is
  always `null` (the column exists, nullable, for Phase 8's real payment
  provider to fill in later).
- **`endDate` computation** uses `MembershipDurationUnit.toPeriod(amount)`,
  mapping each unit to a `java.time.Period` (`DAY`→`Period.ofDays`,
  `WEEK`→`Period.ofWeeks`, `MONTH`→`Period.ofMonths`, `YEAR`→`Period.ofYears`)
  added to `LocalDate.now()` — calendar-aware arithmetic (a 1-`MONTH` plan
  bought on Jan 31 lands on the actual last day of February's rules via
  `Period`, not a fixed 30-day approximation).
- **`MembershipStateMachine`** (`membership.domain`):
  ```
  PENDING → ACTIVE, CANCELLED
  ACTIVE  → FROZEN, CANCELLED, EXPIRED
  FROZEN  → ACTIVE, CANCELLED
  ```
  `PENDING` is unreachable via any code path this phase (purchase jumps
  straight to `ACTIVE`) — it's modeled now purely for forward-compatibility
  with Phase 8, where a real payment flow will create memberships as
  `PENDING` until payment succeeds.
- **Expiry is lazy, not scheduled.** `API_CONTRACT.md` requires that an
  `ACTIVE` membership whose `endDate` has passed reads as `EXPIRED` even
  before any batch job flips it. Rather than introduce a scheduler dependency
  for an MVP phase, every read/mutate path that loads a `Membership`
  (`getOwned`, `listForBusiness`, `listForCustomer`, `cancelByCustomer`) calls
  `applyLazyExpiry()`, which flips the in-memory (and then persisted, via the
  open transaction) status to `EXPIRED` if `status == ACTIVE && endDate <
  today` — bypassing the state machine directly (`Membership.markExpired()`)
  since this is the one system-driven transition, not a client action. The
  tradeoff: a membership nobody reads after its `endDate` will sit with a
  stale `ACTIVE` row indefinitely; this was judged acceptable for the MVP
  since every customer-visible and owner-visible read path already applies
  the check.
- Membership plans are soft-deleted the same way services are — `DELETE
  .../membership-plans/{id}` sets `status = INACTIVE` rather than removing
  the row (so existing purchased `Membership` rows keep a valid
  `membership_plan_id` foreign key).

### Discovery exposes active membership plans publicly

`GET /businesses/{businessId}/membership-plans` is OWNER/STAFF-only (it's the
*management* listing — every status, for editing). That left no way for a
customer to browse a business's plans before purchasing one, since
`POST .../memberships` only accepts a `membershipPlanId` the caller must
already know. `BusinessPublicDto` (returned by both
`GET /discovery/businesses` and `GET /discovery/businesses/{slug}`) therefore
carries a `membershipPlans` field: always a list (never `null`), populated
only when the business has the `MEMBERSHIPS` capability, and only ever
containing `ACTIVE` plans (`MembershipPlanService.listActivePublic`,
mirroring `CatalogService.listActivePublic`'s pattern for services). This is
computed in `DiscoveryService.dto()` for both the list/summary view and the
full slug view, so it appears identically in each.

## 10. What's deferred to later phases

Per the assignment's scope, the following `CLAUDE_CODE.md` entities/phases are
**not** implemented and have **no** database tables yet: `Attendance`,
`Resource` (rentals), `Class`/`ClassEnrollment`, `Review`, `Notification`,
real payment-provider integration, and platform Admin APIs. The
`BusinessCapability` and `BusinessCategory` enums already declare every value
those phases will need (`RENTALS`, `CLASSES`, `CAPACITY`, `RESOURCES`,
`PAYMENTS`), so enabling a capability on a business today is
forward-compatible with those phases without a schema migration to the enum
itself. `ServiceBookingType` similarly already includes `REQUEST`/`RENTAL` —
`QUEUE`/`WALK_IN` now have working queue-join logic as of this pass, and
`APPOINTMENT` has working availability/booking logic, exactly as
`API_CONTRACT.md` specifies.

`Business.status` also already models `SUSPENDED`/`ARCHIVED` for the future
admin/moderation phase, even though this pass only exercises the
`DRAFT → ACTIVE` publish transition.

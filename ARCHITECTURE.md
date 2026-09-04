# ARCHITECTURE.md — Service Business Platform Backend

This document describes the architecture actually implemented for Phases 1-8
(Foundation, Services, Appointments, Queue, Memberships, Gym Operations,
Rental, Reviews, Notifications, Admin, Payments) of `CLAUDE_CODE.md`,
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
attendance  — the Attendance aggregate (check-in/check-out) plus the derived
              CapacityResponse; occupancy is COUNT(*) of open rows, never a
              stored counter
classes     — the ClassSession aggregate + ClassEnrollment, the waitlist
              policy, the owner roster/attendance controllers and the
              customer-facing /me/class-enrollments controller. Package is
              `classes` (plural) and the entity is `ClassSession` because
              `class` is a Java keyword and a type named `Class` would shadow
              `java.lang.Class`; the wire contract still says "class"
resource    — the Resource aggregate (bookable physical assets: JCBs, cars,
              rooms). Rentals are NOT a separate aggregate — they are
              Bookings carrying a `resourceId` (see section 11)
customer    — CustomerProfile (platform-wide identity) + BusinessCustomer
              (per-business relationship), auto-created on first booking
review      — Review aggregate with moderation workflow (PENDING/APPROVED/REJECTED),
              tied to completed bookings, one per booking
notification— Notification entity with type/title/message, in-app delivery,
              read state tracking, bulk mark-all-as-read
admin       — Admin platform stats and business moderation (verify/suspend/activate),
              requires PLATFORM role ADMIN
payment     — Payment aggregate as a separate domain, polymorphic reference
              (BOOKING/MEMBERSHIP/RENTAL/OTHER), simulated provider for MVP
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

## 10. Attendance, capacity and classes (Phase 6)

### Occupancy is always derived, never stored

`CLAUDE_CODE.md` §18-19 is explicit that occupancy must not be stored as the
only source of truth. There is therefore no `current_occupancy` column
anywhere: `GET /businesses/{businessId}/capacity` issues a live
`countByBusinessIdAndCheckOutAtIsNull` and returns
`{ current, capacity, available }`. Only the *configured maximum*
(`business.max_capacity`, nullable, `> 0` when set) is persisted, and it is
edited through the existing `PATCH /businesses/{businessId}` rather than a new
endpoint. When `max_capacity` is unset both `capacity` and `available` are
`null` (unlimited/untracked), never `0` — the arithmetic lives in the pure
`CapacityResponse.of(current, maxCapacity)` factory, so it is unit-testable and
a venue that is over capacity reports `available: 0` rather than a negative
number.

### `ATTENDANCE_CONFLICT`

`API_CONTRACT.md` left the choice open between reusing `BOOKING_CONFLICT` and
adding a new code for "this customer already has an open attendance record".
We added a dedicated **`ATTENDANCE_CONFLICT`** (409), following the existing
`QUEUE_CONFLICT` precedent: a distinct code lets the frontend show "this member
is already checked in" rather than the generic booking-slot message, and the
response body is the same `{timestamp,status,code,message,path}` shape as every
other `ApiException`. It is also raised when checking out an already-closed
record. The duplicate check is serialized by the same Postgres advisory-lock
pattern `BookingService`/`QueueService` use, and is backed by a partial unique
index (`uq_attendance_open_per_customer ... WHERE check_out_at IS NULL`), so a
race that somehow slipped past the application layer still cannot create a
duplicate.

Attendance is deliberately independent of membership (§18): holding an `ACTIVE`
membership says nothing about whether the customer is currently on the
premises, and check-in does not require one.

### Classes: full means waitlist, not reject

`enrolledCount` on `ClassDto` is a live count of `ENROLLED` enrollments, never
a column. Enrolling into a full class produces a `WAITLISTED` enrollment rather
than a 409 (`CLAUDE_CODE.md` §20) — the decision is a one-line pure function,
`ClassEnrollmentPolicy.statusFor(enrolledCount, capacity)`, so the rule has
exactly one implementation and a direct unit test. Enrollment is serialized on
a per-class advisory lock so two simultaneous enrollments into the last seat
cannot both read the same pre-full count.

**Out of scope this phase:** automatic promotion of a `WAITLISTED` enrollment
to `ENROLLED` when someone cancels. Nothing in `API_CONTRACT.md` exposes it,
and doing it properly needs a defined promotion order plus customer
notification (Phase 8). A cancelled seat simply frees capacity for whoever
enrolls next. A duplicate active enrollment for the same (class, customer) is
rejected with `VALIDATION_ERROR`, backed by a partial unique index that
excludes `CANCELLED` rows so a customer who cancels can re-enroll later.

Upcoming (`SCHEDULED`, future `startAt`) classes appear on `BusinessPublicDto`
as a `classes` array, using exactly the same capability-gated pattern as
`membershipPlans` (see section 9's "Discovery exposes active membership plans
publicly").

## 11. Resources and rentals (Phase 7)

### Rentals reuse the Booking aggregate

A rental is not a parallel booking system. `Booking` gained a nullable
`resource_id`, and `BookingService.create` branches on the service's
`bookingType`:

| | APPOINTMENT | RENTAL |
|---|---|---|
| `staffId` | optional | rejected |
| `resourceId` | rejected | required |
| `endAt` | computed from `durationMinutes` (client value ignored) | required from the client, must be after `startAt` |
| `price` | the service's price | `price × ceil(duration / pricingUnit)` |
| business-hours check | enforced | skipped (see below) |

`RENTAL` services carry a new `pricingUnit` (`HOUR`/`DAY`; nullable on the
column, but required for `RENTAL` and validated at service create/update time),
and their `price` is then the **per-unit rate**. The total is computed by the
pure `RentalPricing.totalPrice(...)`, which *ceilings*: a 90-minute hourly hire
is charged 2 hours, a 25-hour daily hire 2 days, and a rental is always charged
at least one whole unit.

The business-hours containment check is deliberately appointment-only. Opening
hours constrain *when a customer collects* an asset, not how long they may hold
it, and a multi-day JCB hire legitimately spans closing time. This is the one
place the two booking kinds diverge in validation, and it is called out in a
comment in `BookingService`.

### One overlap rule for both kinds

`CLAUDE_CODE.md` §34's rule (`requestedStart < existingEnd AND requestedEnd >
existingStart`) now lives in a single pure class,
`BookingConflictDetector.conflicts(start, end, staffId, resourceId,
candidates)`, used by appointments and rentals alike — there is deliberately no
second, subtly-different copy of the rule for rentals. The concurrency-safety
mechanism is unchanged too: the same transaction-scoped Postgres advisory lock
described in section 5, now keyed on `(businessId, staffId, resourceId)`, so
two simultaneous rentals of the same JCB serialize exactly like two
appointments with the same stylist. A rental is additionally rejected when the
target resource's status is `MAINTENANCE` or `UNAVAILABLE`.

### Rental availability reuses the availability endpoint

`GET .../availability?serviceId=&resourceId=&date=` returns the same
`AvailabilityResponse` shape with `type: "RENTAL"`, generating slots against
the resource's existing non-terminal bookings instead of a staff member's.
Two deliberate differences from appointment slots: the window is the whole
calendar day rather than the business-hours intervals (consistent with the
booking-creation rule above), and the slot length is the service's
`pricingUnit` — hourly rentals get 24 hourly slots, daily rentals a single
whole-day slot. `SlotCalculator` was refactored to expose a
`generateWindow(start, end, ...)` primitive that both paths share, so the
overlap/past-slot marking stays identical.

Non-`UNAVAILABLE` resources appear on `BusinessPublicDto` as a `resources`
array, gated on the `RESOURCES` *or* `RENTALS` capability since either implies
a customer-visible fleet, so a customer can pick one before booking.
`DELETE .../resources/{id}` is a soft delete to `UNAVAILABLE`, matching how
services/staff/plans are already retired — historical bookings keep a valid
foreign key.


## 12. Reviews (Phase 8)

Reviews belong to completed customer/business interactions (`CLAUDE_CODE.md` §23).
Each review is tied to a specific booking, ensuring only eligible customers
(completed booking, one per booking) can submit one.

`Review` entity has a `ReviewStatus` enum: `PENDING → APPROVED / REJECTED`.
Reviews start as `PENDING` on customer submission and require business owner
approval or rejection. This provides built-in moderation as required by §23.

The unique constraint on `booking_id` enforces one review per booking at the
database level. The application layer additionally verifies:
- The booking is `COMPLETED`
- The booking belongs to the calling customer
- No prior review exists for this booking+customer pair

## 13. Notifications (Phase 8)

Notifications are an in-app delivery mechanism (`CLAUDE_CODE.md` §24). The
`Notification` entity stores type, title, message, and an optional reference
to the related domain object (e.g., a booking or membership).

Notification generation is triggered by domain events (booking confirmed,
membership activated, etc.) via direct calls from the relevant service classes.
The notification service is decoupled from delivery providers — initial delivery
is in-app only, with the architecture supporting future WebSocket/SSE push
and external providers (SMS, email, WhatsApp).

Read state is tracked via an `is_read` boolean. Bulk mark-all-as-read is
implemented as a single `UPDATE` query.

## 14. Admin APIs (Phase 8)

Admin endpoints are protected by `@PreAuthorize("hasRole('ADMIN')")` and
provide business moderation (verify/reject/suspend/activate) and platform
stats. The admin role is a global `PlatformRole.ADMIN` that bypasses all
business-membership checks (`BusinessAccessService` returns `true`
unconditionally for admins — see section 3).

Admin verification flows: `UNVERIFIED → VERIFIED` or `UNVERIFIED → REJECTED`.
Business suspension: `ACTIVE → SUSPENDED`. Reactivation: `SUSPENDED → ACTIVE`.

## 15. Payments (Phase 8)

Payments are modeled as a separate domain (`CLAUDE_CODE.md` §22) with a clean
service interface. The `Payment` entity tracks amount, currency, status,
provider, and a polymorphic reference (`referenceType` + `referenceId`) to
the related booking, membership, or rental.

For MVP, payments are simulated — the `simulateSuccess` endpoint (admin-only)
marks a `PENDING` payment as `SUCCESS` with a generated provider reference.
This allows the full payment flow to be tested without real provider
integration. The `PaymentService` is designed so a real provider adapter can
be plugged in later without changing callers.

The `paymentId` column on `Membership` (already nullable from Phase 5) can now
be populated when a membership is purchased through a payment flow.

## 16. What's deferred to later phases

The following are **not** implemented:
- Real payment-provider integration (Stripe, Razorpay, etc.) — the interface
  is ready but the adapter is a simulation
- WebSocket/SSE real-time push for notifications and queue updates
- Analytics/reporting dashboard
- File upload for images (logo, cover, resource images)
- Automatic waitlist promotion for classes
- Geo-search for discovery
- Email/SMS/WhatsApp notification delivery

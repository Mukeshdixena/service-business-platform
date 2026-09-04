# API Contract — Service Business Platform (Phase 1-7)

This is the single source of truth for the backend/frontend boundary. Both sides
implement strictly against this document. Anything not listed here is out of
scope for this pass (see CLAUDE_CODE.md phases 4-8).

Base path: `/api/v1`

Conventions:
- All IDs are UUID strings.
- All timestamps are ISO-8601 UTC, e.g. `2026-09-04T14:30:00Z`.
- Money: `price` is a JSON number with up to 2 decimals, plus a sibling `currency`
  field (ISO 4217, e.g. `"INR"`, `"USD"`).
- Pagination query params: `page` (0-based, default 0), `size` (default 20, max 100),
  `sort` (e.g. `sort=createdAt,desc`).
- All list endpoints return a `PageResponse<T>`:
```json
{
  "content": [ ],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "hasNext": false
}
```
- Auth: `Authorization: Bearer <accessToken>`. Refresh token is an httpOnly,
  Secure, SameSite=Strict cookie named `refreshToken`, scoped to
  `/api/v1/auth/refresh`.

## Error format (all non-2xx responses)

```json
{
  "timestamp": "2026-09-04T14:30:00Z",
  "status": 409,
  "code": "BOOKING_CONFLICT",
  "message": "The selected time is no longer available.",
  "path": "/api/v1/businesses/{businessId}/bookings"
}
```

Standard `code` values:
`VALIDATION_ERROR` (400), `UNAUTHENTICATED` (401), `FORBIDDEN` (403),
`NOT_FOUND` (404), `DUPLICATE_SLUG` (409), `BOOKING_CONFLICT` (409),
`QUEUE_CONFLICT` (409 — a second concurrently-active queue entry for the same
customer+business), `INVALID_STATE_TRANSITION` (409), `INTERNAL_ERROR` (500).

Never leak stack traces or internal exception messages.

## Enums

```text
BusinessCategory      SALON, CLINIC, GYM, CAR_RENTAL, BIKE_RENTAL, EQUIPMENT_RENTAL,
                       MECHANIC, SPA, ACADEMY, COWORKING, OTHER
BusinessCapability     APPOINTMENTS, QUEUE, MEMBERSHIPS, RENTALS, CLASSES, CAPACITY,
                       STAFF, RESOURCES, PAYMENTS
BusinessStatus         DRAFT, ACTIVE, SUSPENDED, ARCHIVED
VerificationStatus     UNVERIFIED, PENDING, VERIFIED, REJECTED
PlatformRole           CUSTOMER, BUSINESS_OWNER, STAFF, ADMIN   (User.roles, global)
BusinessMembershipRole OWNER, STAFF                              (per-business role)
DayOfWeek              MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
ServiceBookingType     APPOINTMENT, QUEUE, REQUEST, RENTAL, WALK_IN
                       (only APPOINTMENT has working availability/booking logic
                       this pass; the others are accepted/stored for forward
                       compatibility with later phases)
ServiceStatus          ACTIVE, INACTIVE
StaffStatus            ACTIVE, INACTIVE
BookingStatus          PENDING, CONFIRMED, CHECKED_IN, IN_PROGRESS, COMPLETED,
                       CANCELLED, NO_SHOW, REJECTED
QueueEntryStatus       WAITING, CALLED, SERVING, COMPLETED, SKIPPED, CANCELLED, NO_SHOW
MembershipPlanStatus   ACTIVE, INACTIVE
MembershipDurationUnit DAY, WEEK, MONTH, YEAR
MembershipStatus       PENDING, ACTIVE, EXPIRED, FROZEN, CANCELLED
ClassStatus            SCHEDULED, CANCELLED, COMPLETED
ClassEnrollmentStatus  ENROLLED, WAITLISTED, CANCELLED, ATTENDED, NO_SHOW
ResourceStatus         AVAILABLE, RESERVED, IN_USE, MAINTENANCE, UNAVAILABLE
PricingUnit            HOUR, DAY
```

Queue state machine (server-enforced, mirrors the booking machine's pattern):
```
WAITING -> CALLED, SKIPPED, CANCELLED
CALLED  -> SERVING, NO_SHOW, CANCELLED
SERVING -> COMPLETED
```
Position is never stored as a mutable client-facing fact — it is always derived
server-side from ordering (`joinedAt` ascending) over the business's currently
WAITING/CALLED entries, recomputed on every read (CLAUDE_CODE.md §15).

Membership state machine:
```
PENDING  -> ACTIVE, CANCELLED
ACTIVE   -> FROZEN, CANCELLED, EXPIRED
FROZEN   -> ACTIVE, CANCELLED
```
`EXPIRED` is also reached automatically (not via a client action) once
`endDate` has passed — read paths must treat an ACTIVE membership past its
`endDate` as effectively expired even before any batch job flips the stored
status.

Booking state machine (server-enforced, no arbitrary PATCH of `status`):
```
PENDING     -> CONFIRMED, REJECTED, CANCELLED
CONFIRMED   -> CHECKED_IN, CANCELLED, NO_SHOW
CHECKED_IN  -> IN_PROGRESS, CANCELLED
IN_PROGRESS -> COMPLETED
```
Any other transition -> 409 `INVALID_STATE_TRANSITION`.

## DTOs

### UserDto
```json
{ "id": "uuid", "email": "a@b.com", "fullName": "Jane Doe",
  "roles": ["CUSTOMER"], "createdAt": "..." }
```

### AuthResponse
```json
{ "accessToken": "jwt", "tokenType": "Bearer", "expiresIn": 900, "user": UserDto }
```

### BusinessMembershipDto
```json
{ "businessId": "uuid", "businessName": "...", "role": "OWNER" }
```

### BusinessDto (management — owner/staff view)
```json
{ "id": "uuid", "name": "Glow Salon", "slug": "glow-salon",
  "description": "...", "phone": "...", "email": "...",
  "logoUrl": null, "coverImageUrl": null,
  "category": "SALON", "capabilities": ["APPOINTMENTS", "STAFF"],
  "status": "DRAFT", "verificationStatus": "UNVERIFIED",
  "maxCapacity": null,
  "createdAt": "...", "updatedAt": "..." }
```
`maxCapacity` (nullable integer, > 0 when set) is only meaningful when the
`CAPACITY` capability is enabled; settable via the existing
`PATCH /businesses/{businessId}`/`CreateBusinessRequest`/`UpdateBusinessRequest`
(add it to those request shapes too — no new endpoint needed).

### BusinessPublicDto (discovery — public view; no private fields)
```json
{ "id": "uuid", "name": "...", "slug": "...", "description": "...",
  "phone": "...", "email": "...", "logoUrl": null, "coverImageUrl": null,
  "category": "SALON", "capabilities": ["APPOINTMENTS"],
  "locations": [LocationDto], "services": [ServicePublicDto],
  "staff": [StaffPublicDto], "hours": [BusinessHoursDto],
  "membershipPlans": [MembershipPlanDto],
  "classes": [ClassDto], "resources": [ResourceDto] }
```
`membershipPlans`, `classes`, and `resources` are always present as (possibly
empty) arrays, never null/omitted, and each is only ever non-empty when the
corresponding capability (`MEMBERSHIPS`, `CLASSES`, `RESOURCES`/`RENTALS`) is
enabled — `membershipPlans` only includes `ACTIVE` plans, `classes` only
upcoming `SCHEDULED` ones, `resources` all non-`UNAVAILABLE` ones. This is how
a logged-out or not-yet-a-customer browses before buying/enrolling/renting.
**Correction to an earlier version of this contract**: the
authenticated `GET /businesses/{businessId}/membership-plans` endpoint below
is the OWNER/STAFF *management* listing (all statuses, for editing) — it is
NOT how customers browse plans to purchase. Customers always browse via this
`BusinessPublicDto.membershipPlans` field (full discovery response) or
`GET /discovery/businesses/{slug}`; `POST /businesses/{businessId}/memberships`
(purchase) only needs the `membershipPlanId`, which the customer already has
from that public listing.

### LocationDto
```json
{ "id": "uuid", "businessId": "uuid", "label": "Main branch",
  "addressLine1": "...", "addressLine2": null, "city": "...", "state": "...",
  "postalCode": "...", "country": "...", "latitude": null, "longitude": null,
  "isPrimary": true }
```
`CreateLocationRequest` / `UpdateLocationRequest`: same shape minus `id`, `businessId`.

### BusinessHoursDto
```json
{ "id": "uuid", "businessId": "uuid", "dayOfWeek": "MONDAY",
  "openTime": "06:00", "closeTime": "10:00" }
```
Multiple rows per `dayOfWeek` are allowed (multiple intervals/day).
`UpsertBusinessHoursRequest`: `{ "hours": [ { "dayOfWeek", "openTime", "closeTime" }, ... ] }`
— full replace of the business's hours in one call.

### ServiceDto / ServicePublicDto
```json
{ "id": "uuid", "businessId": "uuid", "name": "Haircut", "description": "...",
  "price": 200.00, "currency": "INR", "durationMinutes": 30,
  "bookingType": "APPOINTMENT", "pricingUnit": null, "status": "ACTIVE",
  "createdAt": "...", "updatedAt": "..." }
```
`ServicePublicDto` omits nothing extra here (services have no private fields),
but is only ever returned for `status = ACTIVE` businesses/services.
`CreateServiceRequest`: `{ name, description, price, currency, durationMinutes
(nullable — required for APPOINTMENT, ignored for RENTAL), bookingType,
pricingUnit (nullable — required for RENTAL: "HOUR" or "DAY"; ignored
otherwise) }`. `UpdateServiceRequest`: all fields optional (partial update) +
`status`. For a `RENTAL`-type service, `price` is the per-`pricingUnit` rate
(e.g. price-per-hour); a rental booking's total `price` = rate × ceil(duration
/ unit), computed server-side.

### StaffDto / StaffPublicDto
```json
{ "id": "uuid", "businessId": "uuid", "userId": null, "displayName": "Asha",
  "title": "Senior Stylist", "bio": "...", "imageUrl": null, "status": "ACTIVE",
  "serviceIds": ["uuid"] }
```
`StaffPublicDto` is identical minus `userId` (never expose internal user linkage
publicly, per §41).
`CreateStaffRequest`: `{ displayName, title, bio, imageUrl, serviceIds }`.
`UpdateStaffRequest`: partial + `status`.

### AvailabilityResponse (GET availability)
```json
{
  "status": "AVAILABLE",
  "type": "APPOINTMENT",
  "date": "2026-09-10",
  "slots": [
    { "start": "2026-09-10T09:00:00Z", "end": "2026-09-10T09:30:00Z",
      "available": true, "staffId": "uuid" }
  ]
}
```
`status` is `AVAILABLE` if any slot is available, else `UNAVAILABLE` (e.g. business
closed that day, or service/staff inactive).

### BookingDto
```json
{ "id": "uuid", "businessId": "uuid", "customerId": "uuid", "serviceId": "uuid",
  "staffId": null, "resourceId": null, "startAt": "...", "endAt": "...",
  "status": "PENDING", "price": 200.00, "currency": "INR", "notes": null,
  "createdAt": "...", "updatedAt": "..." }
```
`CreateBookingRequest`: `{ serviceId, staffId (nullable), resourceId (nullable),
startAt, endAt (nullable — see below), notes (nullable) }` — `price`/`currency`
are always computed server-side from the service, never trusted from the
client. For an `APPOINTMENT`-type service, `endAt` is always computed
server-side from the service's `durationMinutes` (any client-supplied `endAt`
is ignored). **New in Phase 7**: for a `RENTAL`-type service, pass `resourceId`
(required for RENTAL, disallowed for APPOINTMENT) and an explicit `endAt`
(required for RENTAL, since a rental's duration is customer-chosen, not fixed
by the service) — price for a rental is computed server-side from the
service's per-unit price × the requested duration (see `ServiceDto.pricingUnit`
below). The overlap conflict check (§34: `requestedStart < existingEnd AND
requestedEnd > existingStart`) applies identically whether the conflict is
against another booking for the same `staffId` or the same `resourceId`.

### CustomerProfileDto
```json
{ "id": "uuid", "userId": "uuid", "phone": null, "createdAt": "..." }
```

## Endpoints

### Auth — public
```
POST /auth/register   { email, password, fullName, role: "CUSTOMER"|"BUSINESS_OWNER" } -> 201 AuthResponse
POST /auth/login       { email, password }                                             -> 200 AuthResponse
POST /auth/refresh      (reads refreshToken cookie)                                      -> 200 AuthResponse
POST /auth/logout                                                                       -> 204
```

### Users — authenticated
```
GET  /users/me         -> 200 UserDto
GET  /users/me/businesses -> 200 [BusinessMembershipDto]   (businesses the caller owns/staffs)
GET  /users/me/customer-profile -> 200 CustomerProfileDto (created lazily on first call)
```

### Businesses — authenticated, owner-scoped unless noted
```
POST   /businesses                         -> 201 BusinessDto   (caller becomes OWNER)
GET    /businesses/{businessId}            -> 200 BusinessDto   (OWNER/STAFF of that business)
PATCH  /businesses/{businessId}            -> 200 BusinessDto   (OWNER only)
POST   /businesses/{businessId}/publish    -> 200 BusinessDto   (DRAFT -> ACTIVE, OWNER only)

GET    /businesses/{businessId}/locations       -> 200 [LocationDto]
POST   /businesses/{businessId}/locations       -> 201 LocationDto
PATCH  /businesses/{businessId}/locations/{id}  -> 200 LocationDto
DELETE /businesses/{businessId}/locations/{id}  -> 204

GET    /businesses/{businessId}/hours      -> 200 [BusinessHoursDto]
PUT    /businesses/{businessId}/hours      -> 200 [BusinessHoursDto]  (full replace)

GET    /businesses/{businessId}/services       -> 200 PageResponse<ServiceDto>
POST   /businesses/{businessId}/services       -> 201 ServiceDto
PATCH  /businesses/{businessId}/services/{id}  -> 200 ServiceDto
DELETE /businesses/{businessId}/services/{id}  -> 204  (soft: sets INACTIVE)

GET    /businesses/{businessId}/staff       -> 200 PageResponse<StaffDto>
POST   /businesses/{businessId}/staff       -> 201 StaffDto
PATCH  /businesses/{businessId}/staff/{id}  -> 200 StaffDto
DELETE /businesses/{businessId}/staff/{id}  -> 204  (soft: sets INACTIVE)

GET /businesses/{businessId}/availability?serviceId=&staffId=&date=YYYY-MM-DD -> 200 AvailabilityResponse

GET  /businesses/{businessId}/bookings?status=&from=&to=  -> 200 PageResponse<BookingDto>  (OWNER/STAFF)
POST /businesses/{businessId}/bookings/{id}/confirm    -> 200 BookingDto
POST /businesses/{businessId}/bookings/{id}/check-in   -> 200 BookingDto
POST /businesses/{businessId}/bookings/{id}/start      -> 200 BookingDto
POST /businesses/{businessId}/bookings/{id}/complete   -> 200 BookingDto
POST /businesses/{businessId}/bookings/{id}/cancel     { reason } -> 200 BookingDto
POST /businesses/{businessId}/bookings/{id}/no-show    -> 200 BookingDto
POST /businesses/{businessId}/bookings/{id}/reject     { reason } -> 200 BookingDto
```

### Customer-facing bookings — authenticated (CUSTOMER)
```
POST /businesses/{businessId}/bookings          -> 201 BookingDto   (creates as PENDING)
GET  /me/bookings?status=&from=&to=             -> 200 PageResponse<BookingDto>  (own bookings, across all businesses)
POST /me/bookings/{id}/cancel  { reason }        -> 200 BookingDto  (customer-initiated cancel)
```

### Discovery — public, no auth
```
GET /discovery/categories                 -> 200 [{ "value": "SALON", "label": "Salon" }, ...]
GET /discovery/businesses?query=&category=&city=&page=&size= -> 200 PageResponse<BusinessPublicDto> (summary form: no services/staff/hours arrays in list view)
GET /discovery/businesses/{slug}          -> 200 BusinessPublicDto (full, with services/staff/hours)
```
Only businesses with `status = ACTIVE` are ever returned from `/discovery/**`.

### QueueEntryDto
```json
{ "id": "uuid", "businessId": "uuid", "customerId": "uuid", "serviceId": "uuid",
  "staffId": null, "position": 2, "status": "WAITING",
  "joinedAt": "...", "calledAt": null, "startedAt": null, "completedAt": null,
  "estimatedWaitMinutes": 20 }
```
`position` and `estimatedWaitMinutes` are computed on every read, never stored
as authoritative state to mutate directly.
`CreateQueueEntryRequest`: `{ serviceId, staffId (nullable) }` — `businessId`
comes from the path; only allowed when the business has the `QUEUE`
capability and the service's `bookingType` is `QUEUE` or `WALK_IN`.

### QueueStatusResponse (customer-facing "where am I" view, per §36)
```json
{ "status": "WAITING", "peopleAhead": 3, "estimatedWaitMinutes": 20 }
```

### MembershipPlanDto
```json
{ "id": "uuid", "businessId": "uuid", "name": "Monthly", "description": "...",
  "price": 1500.00, "currency": "INR", "duration": 1, "durationUnit": "MONTH",
  "status": "ACTIVE" }
```
`CreateMembershipPlanRequest`/`UpdateMembershipPlanRequest`: same shape minus
`id`/`businessId`; update is partial + `status`.

### MembershipDto
```json
{ "id": "uuid", "businessId": "uuid", "customerId": "uuid",
  "membershipPlanId": "uuid", "startDate": "2026-09-10", "endDate": "2026-10-10",
  "status": "ACTIVE", "paymentId": null }
```
`paymentId` is always `null` in this phase — no real payment provider is
wired up yet (CLAUDE_CODE.md §22 keeps payment behind a service interface;
Phase 8 fills this in). `CreatePurchaseMembershipRequest`: `{ membershipPlanId }`
— purchasing a plan creates the membership as `ACTIVE` immediately (no
payment gate yet), with `startDate = today` and `endDate` computed from the
plan's `duration`/`durationUnit`.

## Queue endpoints
```
POST /businesses/{businessId}/queue                 -> 201 QueueEntryDto   (customer joins; CUSTOMER role)
GET  /businesses/{businessId}/queue?status=          -> 200 [QueueEntryDto] (OWNER/STAFF; ordered by position)
POST /businesses/{businessId}/queue/{id}/call        -> 200 QueueEntryDto
POST /businesses/{businessId}/queue/{id}/start       -> 200 QueueEntryDto
POST /businesses/{businessId}/queue/{id}/complete    -> 200 QueueEntryDto
POST /businesses/{businessId}/queue/{id}/skip        -> 200 QueueEntryDto
POST /businesses/{businessId}/queue/{id}/no-show     -> 200 QueueEntryDto
POST /businesses/{businessId}/queue/{id}/cancel      -> 200 QueueEntryDto  (OWNER/STAFF or the owning customer)

GET  /me/queue-entries                               -> 200 [QueueEntryDto]  (own, across businesses; terminal entries included for history)
GET  /me/queue-entries/{id}/status                   -> 200 QueueStatusResponse
```
The backend must reject a second concurrently-active (`WAITING`/`CALLED`/`SERVING`)
queue entry for the same customer+business (CLAUDE_CODE.md §35 — no duplicate
active entries).

## Membership endpoints
```
GET    /businesses/{businessId}/membership-plans          -> 200 PageResponse<MembershipPlanDto>
POST   /businesses/{businessId}/membership-plans          -> 201 MembershipPlanDto   (OWNER)
PATCH  /businesses/{businessId}/membership-plans/{id}     -> 200 MembershipPlanDto   (OWNER)
DELETE /businesses/{businessId}/membership-plans/{id}     -> 204  (soft: sets INACTIVE)

POST /businesses/{businessId}/memberships                 -> 201 MembershipDto  (customer purchases a plan)
GET  /businesses/{businessId}/memberships?status=          -> 200 PageResponse<MembershipDto>  (OWNER/STAFF)
POST /businesses/{businessId}/memberships/{id}/freeze      -> 200 MembershipDto  (OWNER/STAFF)
POST /businesses/{businessId}/memberships/{id}/reactivate  -> 200 MembershipDto  (OWNER/STAFF)
POST /businesses/{businessId}/memberships/{id}/cancel      -> 200 MembershipDto  (OWNER/STAFF, or the owning customer via /me)

GET  /me/memberships                                       -> 200 [MembershipDto]  (own, across businesses)
```

### AttendanceDto
```json
{ "id": "uuid", "businessId": "uuid", "customerId": "uuid",
  "checkInAt": "...", "checkOutAt": null }
```
`CreateAttendanceRequest` (check-in): `{ customerId }`. A customer may not have
more than one open (`checkOutAt: null`) attendance record per business at a
time — reject a second check-in with 409 (reuse `BOOKING_CONFLICT`-style
handling or add an `ATTENDANCE_CONFLICT` code, your call, document whichever).

### CapacityResponse
```json
{ "current": 87, "capacity": 150, "available": 63 }
```
`current` = count of currently-open (`checkOutAt: null`) attendance records
for the business (CLAUDE_CODE.md §18-19 — occupancy is always derived, never
a separately-stored counter). `capacity` is the business's `maxCapacity`; if
unset, return `capacity: null, available: null`.

### ClassDto
```json
{ "id": "uuid", "businessId": "uuid", "name": "Morning Yoga", "description": "...",
  "staffId": null, "startAt": "...", "endAt": "...", "capacity": 20,
  "enrolledCount": 5, "status": "SCHEDULED" }
```
`enrolledCount` (derived, `ENROLLED` status only) is always included so the
UI can show "5/20" without a second call. `CreateClassRequest`/
`UpdateClassRequest`: same shape minus `id`/`businessId`/`enrolledCount`;
update is partial + `status`.

### ClassEnrollmentDto
```json
{ "id": "uuid", "classId": "uuid", "customerId": "uuid", "status": "ENROLLED",
  "createdAt": "..." }
```
Enrolling when `enrolledCount >= capacity` creates the enrollment as
`WAITLISTED` instead of rejecting it (CLAUDE_CODE.md §20).

### ResourceDto
```json
{ "id": "uuid", "businessId": "uuid", "name": "JCB-01", "type": "EXCAVATOR",
  "description": "...", "imageUrl": null, "identifier": "JCB-01",
  "status": "AVAILABLE" }
```
`CreateResourceRequest`/`UpdateResourceRequest`: same shape minus `id`/`businessId`;
update is partial + `status`. A resource in `MAINTENANCE`/`UNAVAILABLE` status
cannot be booked (checked at booking-creation time, in addition to the normal
overlap check).

## Attendance & Capacity endpoints
```
POST /businesses/{businessId}/attendance/check-in       -> 201 AttendanceDto   (OWNER/STAFF)
POST /businesses/{businessId}/attendance/{id}/check-out -> 200 AttendanceDto   (OWNER/STAFF)
GET  /businesses/{businessId}/attendance?activeOnly=     -> 200 PageResponse<AttendanceDto>  (OWNER/STAFF)
GET  /businesses/{businessId}/capacity                   -> 200 CapacityResponse  (OWNER/STAFF; public capacity display is a Phase 8+ discovery concern if ever needed — not exposed publicly this phase)
```

## Class endpoints
```
GET    /businesses/{businessId}/classes?status=&from=&to=   -> 200 PageResponse<ClassDto>  (OWNER/STAFF)
POST   /businesses/{businessId}/classes                     -> 201 ClassDto   (OWNER)
PATCH  /businesses/{businessId}/classes/{id}                -> 200 ClassDto   (OWNER)
DELETE /businesses/{businessId}/classes/{id}                -> 204  (soft: sets CANCELLED)
GET    /businesses/{businessId}/classes/{id}/enrollments     -> 200 [ClassEnrollmentDto]  (OWNER/STAFF roster)
POST   /businesses/{businessId}/classes/{id}/enrollments/{enrollmentId}/attended -> 200 ClassEnrollmentDto
POST   /businesses/{businessId}/classes/{id}/enrollments/{enrollmentId}/no-show  -> 200 ClassEnrollmentDto

POST /businesses/{businessId}/classes/{id}/enroll            -> 201 ClassEnrollmentDto  (customer; ENROLLED or WAITLISTED per capacity)
POST /businesses/{businessId}/classes/{id}/cancel-enrollment  -> 200 ClassEnrollmentDto  (customer, own enrollment only)
GET  /me/class-enrollments                                    -> 200 [ClassEnrollmentDto]  (own, across businesses)
```
Upcoming (`status: "SCHEDULED"`, `startAt` in the future) classes for
`CLASSES`-capable businesses are also included in `BusinessPublicDto` /
`GET /discovery/businesses/{slug}` as a `classes: [ClassDto]` field (same
"only if capability enabled" pattern as `membershipPlans`), so customers can
browse before enrolling — add this field alongside `membershipPlans`.

## Resource endpoints
```
GET    /businesses/{businessId}/resources        -> 200 PageResponse<ResourceDto>  (OWNER/STAFF)
POST   /businesses/{businessId}/resources        -> 201 ResourceDto   (OWNER)
PATCH  /businesses/{businessId}/resources/{id}   -> 200 ResourceDto   (OWNER)
DELETE /businesses/{businessId}/resources/{id}   -> 204  (soft: sets UNAVAILABLE)
```
Resources are also listed in `BusinessPublicDto` (a `resources: [ResourceDto]`
field, same capability-gated pattern) when `RESOURCES`/`RENTALS` is enabled,
so a customer can pick one when creating a rental booking.

**Rental availability** reuses the existing availability endpoint:
`GET /businesses/{businessId}/availability?serviceId=&resourceId=&date=` (pass
`resourceId` instead of `staffId` for a RENTAL-type service) — same
`AvailabilityResponse` shape, with slots computed against the resource's
existing bookings instead of a staff member's.

## Authorization rules

- `POST /businesses`: any authenticated user with role `BUSINESS_OWNER` or `ADMIN`.
- Every `/businesses/{businessId}/**` management endpoint: caller must have a
  `BusinessMembership` on `businessId` (OWNER for mutating business/services/staff/hours/locations;
  OWNER or STAFF for reading and for booking-lifecycle actions). Never trust
  `businessId` from the URL alone — always re-check membership server-side.
- `/me/**`: caller can only ever see/act on their own `customerId`.
- `ADMIN` bypasses business-membership checks (platform admin).
- Queue/membership mutation endpoints under `/businesses/{businessId}/**` follow
  the same OWNER/STAFF membership check as bookings; `/me/**` queue/membership
  reads and self-cancel actions are scoped to the caller's own `customerId`.
- Attendance/capacity/classes(management)/resources endpoints follow the same
  OWNER/STAFF pattern; class enroll/cancel-enrollment and `/me/class-enrollments`
  follow the same customer-self-scoping as queue/memberships.

## Validation summary

- `email`: valid format, unique.
- `password`: min 8 chars.
- `price`: >= 0.
- `durationMinutes`: > 0 when `bookingType = APPOINTMENT`.
- `startAt` (booking): must be in the future, within business hours, and not
  overlapping an existing non-terminal booking for the same staff (if `staffId`
  set) — see CLAUDE_CODE.md §33.
- `openTime < closeTime` per `BusinessHours` row.
- `slug`: lowercase, `[a-z0-9-]+`, unique, generated from `name` on create if not
  supplied, re-checked/suffixed on collision.
- `duration` (membership plan): > 0.
- A customer may not have more than one concurrently-active queue entry per
  business, and joining requires the `QUEUE` capability enabled.
- Purchasing a membership requires the `MEMBERSHIPS` capability enabled and
  the target plan to be `ACTIVE`.
- `maxCapacity` (business): > 0 when set.
- A customer may not have more than one open attendance record per business.
- A `RENTAL`-type service booking requires `resourceId` + explicit `endAt`;
  an `APPOINTMENT`-type booking must NOT include `resourceId`. `endAt` for a
  rental must be after `startAt`.
- Enrolling in a class beyond `capacity` waitlists rather than rejects; a
  `CANCELLED`/`COMPLETED` class cannot accept new enrollments.

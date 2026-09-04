# API.md — Orientation

The authoritative REST API contract for Phases 1-3 is
[`docs/API_CONTRACT.md`](docs/API_CONTRACT.md) — every endpoint path, DTO field
name, enum value, status code, and validation rule the backend implements
comes from that document verbatim. This file is a short map to help a reader
find their way around it and the implementation; it does not duplicate the
contract's content.

## Base path & conventions

- Base path: `/api/v1`.
- All ids are UUID strings; all timestamps are ISO-8601 UTC.
- Money is a `price` number plus a sibling `currency` (ISO 4217) field.
- Every list endpoint returns a `PageResponse<T>` — see
  `com.platform.common.pagination.PageResponse` for the backend-side shape.
- Every error response has the shape `{ timestamp, status, code, message, path }`
  — see `com.platform.common.exception.GlobalExceptionHandler` for how each
  exception type maps to it, and `com.platform.common.response.ErrorResponse`
  for the record itself.

## Endpoint groups → backend controllers

| Contract section | Controller(s) |
|---|---|
| `POST/GET /auth/**` | `auth.controller.AuthController` |
| `GET /users/me`, `/me/businesses`, `/me/customer-profile` | `user.controller.UserController` |
| `POST/GET/PATCH /businesses/{id}`, `/publish` | `business.controller.BusinessController` |
| `/businesses/{id}/locations` | `business.controller.BusinessLocationController` |
| `/businesses/{id}/hours` | `business.controller.BusinessHoursController` |
| `/businesses/{id}/services` | `catalog.controller.ServiceController` |
| `/businesses/{id}/staff` | `staff.controller.StaffController` |
| `/businesses/{id}/availability` | `availability.controller.AvailabilityController` |
| `/businesses/{id}/bookings/**` (owner/staff management) | `booking.controller.BookingController` |
| `/me/bookings/**` (customer-facing) | `booking.controller.MyBookingsController` |
| `/discovery/**` | `business.controller.DiscoveryController` |

## Authentication & authorization, in one paragraph

`Authorization: Bearer <accessToken>` on every non-public request; the token
is a short-lived JWT validated by `common.security.JwtAuthenticationFilter`.
The refresh token is a separate, opaque, revocable, httpOnly cookie (see
`ARCHITECTURE.md` §7). Every `/businesses/{businessId}/**` management endpoint
is gated by `@PreAuthorize("@businessAccessService.hasRole(#businessId,
authentication, 'OWNER'|'STAFF')")`, which re-verifies the caller's
`BusinessMembership` on that exact business server-side on every call — see
`ARCHITECTURE.md` §3 for why and `CrossTenantAuthorizationIntegrationTest` for
the negative-path proof.

## Booking state machine

Implemented exactly as specified in `API_CONTRACT.md` (`PENDING → CONFIRMED /
REJECTED / CANCELLED`, etc.) by `booking.domain.BookingStateMachine`; any
transition not in that table is rejected with 409 `INVALID_STATE_TRANSITION`.
See `ARCHITECTURE.md` §4-5 for the full lifecycle and the concurrency-safety
approach for booking creation.

## Where validation rules live

Bean Validation annotations on request DTOs (`@NotBlank`, `@Email`,
`@DecimalMin`, `@Future`, ...) cover what annotations can express; everything
else (slug generation/uniqueness, `openTime < closeTime`, booking overlap,
business-hours containment, conditional `durationMinutes` requirement for
`APPOINTMENT` services) is enforced in the relevant `*Service` class and
surfaces as `VALIDATION_ERROR`/`BOOKING_CONFLICT`/`DUPLICATE_SLUG` per the
contract's error code table.

## Known, documented deviations from a literal reading of `CLAUDE_CODE.md`

These follow `API_CONTRACT.md` where the two documents could be read
differently — the contract wins per this task's instructions:

- `UpdateBusinessRequest` excludes `slug` and `category` — both are treated as
  immutable after creation (see `ARCHITECTURE.md` §2/§6 discussion in the code
  comments on that DTO).
- Booking creation is restricted to services with `bookingType = APPOINTMENT`,
  since `API_CONTRACT.md` states only that type has working availability/
  booking logic this pass; booking a `QUEUE`/`REQUEST`/`RENTAL`/`WALK_IN`
  service returns `400 VALIDATION_ERROR`.
- `GET .../availability` and `POST .../bookings` (customer creation) are
  reachable by any authenticated user, not gated by business membership —
  they are customer-facing actions nested under the business path, not
  "management" endpoints, so the blanket membership rule in
  `API_CONTRACT.md`'s Authorization section does not apply to them.

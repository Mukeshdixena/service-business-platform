# DATABASE.md — Schema Overview (Phases 1-5)

PostgreSQL, schema owned entirely by Flyway migrations under
`backend/src/main/resources/db/migration/` (`V1` … `V7`). Hibernate's
`ddl-auto` is `validate` in every profile — the application never generates or
alters schema itself (`CLAUDE_CODE.md` §38). Every table has server-generated
`UUID` primary keys (never sequential integers, to avoid IDOR-by-enumeration —
`CLAUDE_CODE.md` §31) and `created_at`/`updated_at` timestamptz columns.

## V1 — Users & auth

| Table | Purpose |
|---|---|
| `app_user` | Platform user identity (email, BCrypt password hash, full name). Named `app_user` because `USER` is a reserved word in PostgreSQL. |
| `user_role` | One row per `(user_id, role)` — a user's global `PlatformRole`s (`CUSTOMER`, `BUSINESS_OWNER`, `STAFF`, `ADMIN`). A user can hold more than one. |
| `refresh_token` | Opaque, server-tracked refresh tokens, stored as a SHA-256 hash (`token_hash`, unique) with `expires_at`/`revoked_at` — never the raw token. |

Indexes: `uq_app_user_email` (unique), `uq_refresh_token_hash` (unique),
`idx_refresh_token_user_id`.

## V2 — Business core

| Table | Purpose |
|---|---|
| `business` | The tenant/business aggregate: name, slug, contact info, `category` (enum), `status` (`DRAFT`/`ACTIVE`/`SUSPENDED`/`ARCHIVED`), `verification_status`. |
| `business_capability` | One row per `(business_id, capability)` — the enabled `BusinessCapability` set. |
| `business_location` | A business's physical location(s) — a business can have more than one. |
| `business_hours` | One row per open/close interval per day-of-week. Multiple rows per `(business_id, day_of_week)` are allowed by design (e.g. a lunch closure = two intervals). `CHECK (open_time < close_time)` enforces the ordering at the DB level in addition to the service-layer check. |
| `business_membership` | **The tenant-boundary join.** `(business_id, user_id, role)` where role is `OWNER` or `STAFF`. Every authorization check for a business-scoped endpoint queries this table — see `ARCHITECTURE.md` §3. |

Indexes:
- `uq_business_slug` (unique) — the public identifier used in discovery URLs.
- `idx_business_category`, `idx_business_status` — per `CLAUDE_CODE.md` §38's
  `business.category_id` guidance; category is modeled as an enum column
  rather than a separate lookup table (matching `API_CONTRACT.md`, which
  returns/accepts category as a plain string enum), but is still indexed for
  the discovery search/filter path.
- `idx_business_location_business_id`, `idx_business_location_city` — city is
  a discovery filter (`GET /discovery/businesses?city=`).
- `idx_business_hours_business_day` — the availability day-of-week lookup.
- `uq_business_membership_business_user` (unique) — a user has at most one
  role per business. `idx_business_membership_user_id` serves
  `GET /users/me/businesses`; `idx_business_membership_business_id` serves the
  per-request authorization lookup.

## V3 — Catalog (bookable services) & staff

| Table | Purpose |
|---|---|
| `service` | The bookable-item entity (named `service` in SQL; the Java entity lives in the `catalog` package to avoid colliding with `@Service`). Price, currency, optional duration, `booking_type`, `status`. `CHECK (price >= 0)`. |
| `staff_member` | A person who performs/manages services at a business. Optional nullable `user_id` link to a platform account. |
| `staff_service` | Many-to-many join: which staff can perform which services. `UNIQUE (staff_id, service_id)`. |

Indexes: `idx_service_business_id`, `idx_service_business_status` (the
"list active services for a business" path used by discovery and catalog
listing), `idx_staff_member_business_id`, `idx_staff_member_business_status`,
`idx_staff_service_staff_id`, `idx_staff_service_service_id`.

## V4 — Customers

| Table | Purpose |
|---|---|
| `customer_profile` | One row per platform `User` who has ever acted as a customer (`CLAUDE_CODE.md` §21) — created lazily on first use, not eagerly for every registered user. `UNIQUE (user_id)`. |
| `business_customer` | The per-business relationship: "this customer has interacted with this business." Auto-created the first time a customer books at a given business (idempotent thereafter). `UNIQUE (business_id, customer_profile_id)`. |

This two-level structure (`User` → `CustomerProfile` → `BusinessCustomer`)
means core identity fields are never duplicated per business, and a single
platform user can have independent relationships with many businesses.

Indexes: `idx_business_customer_business_id`,
`idx_business_customer_customer_profile_id`.

## V5 — Bookings

| Table | Purpose |
|---|---|
| `booking` | The appointment reservation. `customer_id` references `customer_profile.id` (not `app_user.id` directly) so a booking is always tied to the platform-wide customer identity, consistent with how `/me/bookings` aggregates a customer's bookings across every business. `staff_id` is nullable — not every booking needs a staff member. `status` is the state-machine-governed lifecycle value. `CHECK (end_at > start_at)`. |

Indexes:
- `idx_booking_business_start` on `(business_id, start_at)` — named explicitly
  per `CLAUDE_CODE.md` §38, serves both the owner's booking-list/date-range
  queries and the availability day-window lookup.
- `idx_booking_business_status` on `(business_id, status)` — filtering by
  status is a listed query parameter (`GET .../bookings?status=`).
- `idx_booking_customer_id` — serves `GET /me/bookings`.
- `idx_booking_staff_id` — serves per-staff overlap/availability lookups.

`price`/`currency` on a booking are a snapshot of the service's price at
booking time (never recomputed later, and never accepted from the client —
see `ARCHITECTURE.md` §5).

## V6 — Queue

| Table | Purpose |
|---|---|
| `queue_entry` | A customer waiting for walk-in/queue-type service (`CLAUDE_CODE.md` §15-16). `customer_id` references `customer_profile.id`, same pattern as `booking`. `staff_id` nullable. `status` is the state-machine-governed lifecycle value (`WAITING`/`CALLED`/`SERVING`/`COMPLETED`/`SKIPPED`/`CANCELLED`/`NO_SHOW`). There is **no `position` column** — position is never stored as authoritative state; the service layer always derives the 1-based rank from `joined_at` ordering over the business's currently `WAITING`/`CALLED` rows on every read (`API_CONTRACT.md`). |

Indexes:
- `idx_queue_entry_business_status` on `(business_id, status)` — named
  explicitly per `CLAUDE_CODE.md` §38's `queue_entry.business_id + status`
  guidance; serves `GET .../queue?status=`.
- `idx_queue_entry_business_joined_at` — serves the position-derivation query
  (`ORDER BY joined_at ASC` over active statuses for a business).
- `idx_queue_entry_customer_id` — serves `GET /me/queue-entries` and the
  duplicate-active-entry check.

## V7 — Memberships

| Table | Purpose |
|---|---|
| `membership_plan` | A business's sellable membership tier (`CLAUDE_CODE.md` §17): name, price/currency, `duration`/`duration_unit`, `status` (`ACTIVE`/`INACTIVE`). `CHECK (duration > 0)`. |
| `membership` | A customer's purchased membership. `membership_plan_id` and `customer_id` (→ `customer_profile.id`) are both foreign keys; `start_date`/`end_date` are `DATE` (not timestamptz) since a membership's validity is day-granular. `payment_id` is nullable and always `null` this phase — no payment provider is wired up yet (`CLAUDE_CODE.md` §22). `CHECK (end_date >= start_date)`. |

Indexes:
- `idx_membership_business_customer` on `(business_id, customer_id)` — named
  explicitly per `CLAUDE_CODE.md` §38's
  `membership.business_id + customer_id` guidance.
- `idx_membership_business_status` — serves `GET .../memberships?status=`.
- `idx_membership_customer_id` — serves `GET /me/memberships`.
- `idx_membership_plan_business_id`.

`membership.status` transitioning `ACTIVE` → `EXPIRED` once `end_date` has
passed is handled lazily on read (`MembershipService#applyLazyExpiry`), not by
a scheduled job or a DB trigger — the simplest option that satisfies
`API_CONTRACT.md`'s "an ACTIVE membership past its endDate must read as
EXPIRED" rule without adding a scheduler dependency for an MVP phase. This
means a stale `status = ACTIVE` row can sit in the table indefinitely until
something reads it, which is an accepted tradeoff for this phase (see
`ARCHITECTURE.md`).

## Relationships at a glance

```
app_user ──< user_role
         ──< business_membership >── business
         ──< refresh_token
         ──< customer_profile ──< business_customer >── business
                              └─< booking (customer_id)

business ──< business_capability
         ──< business_location
         ──< business_hours
         ──< service ──< staff_service >── staff_member ──< business_membership? (no — staff_member.user_id is a separate optional link)
         ──< staff_member
         ──< booking (business_id, service_id, staff_id nullable)
         ──< queue_entry (business_id, service_id, staff_id nullable)
         ──< membership_plan ──< membership (business_id, membership_plan_id, customer_id)
```

## Deliberate omissions this phase

No tables exist yet for `attendance`, `resource`, `class`, `class_enrollment`,
`review`, `notification`, or `payment` — these belong to Phases 6-8 per
`CLAUDE_CODE.md` §42 and are intentionally out of scope for this migration
set (see `ARCHITECTURE.md` §8). No `CASCADE` deletes are configured on any
foreign key: business/service/staff deletion is not implemented in this phase
(services, staff, and membership plans are soft-deleted via a `status` flip to
`INACTIVE`), so there was no scenario yet that required cascading behavior,
and adding it without a real delete path would only obscure future intent.

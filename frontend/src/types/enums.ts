/**
 * Enum-like string unions mirroring API_CONTRACT.md "## Enums" byte-for-byte.
 * Each union is paired with a `const` array of its values (used for <select>
 * options, validation, etc.) so the two never drift apart.
 */

export const BUSINESS_CATEGORIES = [
  'SALON',
  'CLINIC',
  'GYM',
  'CAR_RENTAL',
  'BIKE_RENTAL',
  'EQUIPMENT_RENTAL',
  'MECHANIC',
  'SPA',
  'ACADEMY',
  'COWORKING',
  'OTHER',
] as const
export type BusinessCategory = (typeof BUSINESS_CATEGORIES)[number]

export const BUSINESS_CAPABILITIES = [
  'APPOINTMENTS',
  'QUEUE',
  'MEMBERSHIPS',
  'RENTALS',
  'CLASSES',
  'CAPACITY',
  'STAFF',
  'RESOURCES',
  'PAYMENTS',
] as const
export type BusinessCapability = (typeof BUSINESS_CAPABILITIES)[number]

export const BUSINESS_STATUSES = ['DRAFT', 'ACTIVE', 'SUSPENDED', 'ARCHIVED'] as const
export type BusinessStatus = (typeof BUSINESS_STATUSES)[number]

export const VERIFICATION_STATUSES = ['UNVERIFIED', 'PENDING', 'VERIFIED', 'REJECTED'] as const
export type VerificationStatus = (typeof VERIFICATION_STATUSES)[number]

/** User.roles — global platform roles. */
export const PLATFORM_ROLES = ['CUSTOMER', 'BUSINESS_OWNER', 'STAFF', 'ADMIN'] as const
export type PlatformRole = (typeof PLATFORM_ROLES)[number]

/** Per-business role, as returned in BusinessMembershipDto.role. */
export const BUSINESS_MEMBERSHIP_ROLES = ['OWNER', 'STAFF'] as const
export type BusinessMembershipRole = (typeof BUSINESS_MEMBERSHIP_ROLES)[number]

export const DAYS_OF_WEEK = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
] as const
export type DayOfWeek = (typeof DAYS_OF_WEEK)[number]

/**
 * Only APPOINTMENT has working availability/booking logic this pass; the
 * others are accepted/stored for forward compatibility with later phases.
 */
export const SERVICE_BOOKING_TYPES = ['APPOINTMENT', 'QUEUE', 'REQUEST', 'RENTAL', 'WALK_IN'] as const
export type ServiceBookingType = (typeof SERVICE_BOOKING_TYPES)[number]

export const SERVICE_STATUSES = ['ACTIVE', 'INACTIVE'] as const
export type ServiceStatus = (typeof SERVICE_STATUSES)[number]

export const STAFF_STATUSES = ['ACTIVE', 'INACTIVE'] as const
export type StaffStatus = (typeof STAFF_STATUSES)[number]

export const BOOKING_STATUSES = [
  'PENDING',
  'CONFIRMED',
  'CHECKED_IN',
  'IN_PROGRESS',
  'COMPLETED',
  'CANCELLED',
  'NO_SHOW',
  'REJECTED',
] as const
export type BookingStatus = (typeof BOOKING_STATUSES)[number]

export const QUEUE_ENTRY_STATUSES = [
  'WAITING',
  'CALLED',
  'SERVING',
  'COMPLETED',
  'SKIPPED',
  'CANCELLED',
  'NO_SHOW',
] as const
export type QueueEntryStatus = (typeof QUEUE_ENTRY_STATUSES)[number]

export const MEMBERSHIP_PLAN_STATUSES = ['ACTIVE', 'INACTIVE'] as const
export type MembershipPlanStatus = (typeof MEMBERSHIP_PLAN_STATUSES)[number]

export const MEMBERSHIP_DURATION_UNITS = ['DAY', 'WEEK', 'MONTH', 'YEAR'] as const
export type MembershipDurationUnit = (typeof MEMBERSHIP_DURATION_UNITS)[number]

export const MEMBERSHIP_STATUSES = ['PENDING', 'ACTIVE', 'EXPIRED', 'FROZEN', 'CANCELLED'] as const
export type MembershipStatus = (typeof MEMBERSHIP_STATUSES)[number]

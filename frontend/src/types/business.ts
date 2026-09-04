import type { MembershipPlanDto } from './membership'
import type { ServicePublicDto } from './service'
import type { StaffPublicDto } from './staff'
import type { BusinessCapability, BusinessCategory, BusinessStatus, DayOfWeek, VerificationStatus } from './enums'

/** Management view (owner/staff) — GET /businesses/{businessId}. */
export interface BusinessDto {
  id: string
  name: string
  slug: string
  description: string | null
  phone: string | null
  email: string | null
  logoUrl: string | null
  coverImageUrl: string | null
  category: BusinessCategory
  capabilities: BusinessCapability[]
  status: BusinessStatus
  verificationStatus: VerificationStatus
  createdAt: string
  updatedAt: string
}

/** Public discovery view — no private fields (API_CONTRACT.md / CLAUDE_CODE.md §41). */
export interface BusinessPublicDto {
  id: string
  name: string
  slug: string
  description: string | null
  phone: string | null
  email: string | null
  logoUrl: string | null
  coverImageUrl: string | null
  category: BusinessCategory
  capabilities: BusinessCapability[]
  /** Present on the full detail view (GET by slug); omitted on the list/summary view. */
  locations?: LocationDto[]
  services?: ServicePublicDto[]
  staff?: StaffPublicDto[]
  hours?: BusinessHoursDto[]
  /**
   * Only populated when the business has the MEMBERSHIPS capability, and only
   * ever contains ACTIVE plans — this is the sole public browsing path for a
   * customer to see plans before buying (the authenticated
   * GET /businesses/{businessId}/membership-plans endpoint is OWNER/STAFF-only
   * management listing, not for customer browsing).
   */
  membershipPlans?: MembershipPlanDto[]
}

export interface LocationDto {
  id: string
  businessId: string
  label: string
  addressLine1: string
  addressLine2: string | null
  city: string
  state: string
  postalCode: string
  country: string
  latitude: number | null
  longitude: number | null
  isPrimary: boolean
}

export type CreateLocationRequest = Omit<LocationDto, 'id' | 'businessId'>
export type UpdateLocationRequest = Partial<CreateLocationRequest>

export interface BusinessHoursDto {
  id: string
  businessId: string
  dayOfWeek: DayOfWeek
  /** "HH:mm" 24-hour, e.g. "06:00". */
  openTime: string
  closeTime: string
}

export interface UpsertBusinessHoursRequest {
  hours: Array<Pick<BusinessHoursDto, 'dayOfWeek' | 'openTime' | 'closeTime'>>
}

/**
 * The contract documents the BusinessDto response shape but not the create/update
 * request bodies. Inferred conservatively from the response DTO and §6/§25:
 * category is fixed at creation time (changing a business's category later is
 * out of scope for this pass); capabilities default server-side and are not
 * editable from this UI yet (Phase 4+ features gate on them).
 */
export interface CreateBusinessRequest {
  name: string
  description?: string | null
  phone?: string | null
  email?: string | null
  category: BusinessCategory
}

export interface UpdateBusinessRequest {
  name?: string
  description?: string | null
  phone?: string | null
  email?: string | null
  logoUrl?: string | null
  coverImageUrl?: string | null
}

import type { BookingListParams, BusinessSearchParams, MembershipListParams, QueueEntryListParams } from '../types'

/**
 * Central registry of react-query keys. Keeping these in one place (rather
 * than inline arrays scattered per component) makes cache invalidation after
 * mutations predictable and lets hooks/useEntityLookup.ts scan the cache by
 * a known prefix.
 */
export const queryKeys = {
  discoveryCategories: ['discovery', 'categories'] as const,
  discoveryBusinesses: (params: BusinessSearchParams) => ['discovery', 'businesses', params] as const,
  discoveryBusinessBySlug: (slug: string) => ['discovery', 'business', slug] as const,

  me: ['users', 'me'] as const,
  myBusinesses: ['users', 'me', 'businesses'] as const,
  myCustomerProfile: ['users', 'me', 'customer-profile'] as const,
  myBookings: (params: BookingListParams) => ['me', 'bookings', params] as const,

  business: (businessId: string) => ['businesses', businessId] as const,
  businessLocations: (businessId: string) => ['businesses', businessId, 'locations'] as const,
  businessHours: (businessId: string) => ['businesses', businessId, 'hours'] as const,
  services: (businessId: string, page: number) => ['businesses', businessId, 'services', page] as const,
  staff: (businessId: string, page: number) => ['businesses', businessId, 'staff', page] as const,
  availability: (businessId: string, params: { serviceId: string; staffId?: string; date: string }) =>
    ['businesses', businessId, 'availability', params] as const,
  businessBookings: (businessId: string, params: BookingListParams) =>
    ['businesses', businessId, 'bookings', params] as const,

  businessQueue: (businessId: string, params?: QueueEntryListParams) =>
    ['businesses', businessId, 'queue', params] as const,
  myQueueEntries: ['me', 'queue-entries'] as const,
  queueEntryStatus: (entryId: string) => ['me', 'queue-entries', entryId, 'status'] as const,

  membershipPlans: (businessId: string, page: number) => ['businesses', businessId, 'membership-plans', page] as const,
  businessMemberships: (businessId: string, params: MembershipListParams) =>
    ['businesses', businessId, 'memberships', params] as const,
  myMemberships: ['me', 'memberships'] as const,
}

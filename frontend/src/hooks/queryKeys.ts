import type {
  AttendanceListParams,
  BookingListParams,
  BusinessSearchParams,
  ClassListParams,
  MembershipListParams,
  QueueEntryListParams,
  ReviewListParams,
} from '../types'

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

  businessAttendance: (businessId: string, params?: AttendanceListParams) =>
    ['businesses', businessId, 'attendance', params] as const,
  businessCapacity: (businessId: string) => ['businesses', businessId, 'capacity'] as const,

  businessClasses: (businessId: string, params?: ClassListParams) =>
    ['businesses', businessId, 'classes', params] as const,
  classEnrollments: (businessId: string, classId: string) =>
    ['businesses', businessId, 'classes', classId, 'enrollments'] as const,
  myClassEnrollments: ['me', 'class-enrollments'] as const,

  businessResources: (businessId: string, page: number) => ['businesses', businessId, 'resources', page] as const,

  businessReviews: (businessId: string, params?: ReviewListParams) =>
    ['businesses', businessId, 'reviews', params] as const,
  myReviews: (params?: ReviewListParams) => ['me', 'reviews', params] as const,

  notifications: (params?: { page?: number; size?: number }) => ['me', 'notifications', params] as const,
  notificationsUnreadCount: ['me', 'notifications', 'unread-count'] as const,

  adminStats: ['admin', 'stats'] as const,
  adminBusinesses: (params?: { status?: string; page?: number }) => ['admin', 'businesses', params] as const,
}

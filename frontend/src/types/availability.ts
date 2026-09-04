export interface AvailabilitySlot {
  start: string
  end: string
  available: boolean
  staffId: string | null
}

/** Rentals reuse the same slot shape; a resource-scoped call has no staffId to report. */

export type AvailabilityStatus = 'AVAILABLE' | 'UNAVAILABLE'

export interface AvailabilityResponse {
  status: AvailabilityStatus
  /** "APPOINTMENT" for staff-scoped availability, "RENTAL" for resource-scoped (Phase 7). */
  type: 'APPOINTMENT' | 'RENTAL'
  date: string
  slots: AvailabilitySlot[]
}

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
  type: 'APPOINTMENT'
  date: string
  slots: AvailabilitySlot[]
}

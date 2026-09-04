export interface AvailabilitySlot {
  start: string
  end: string
  available: boolean
  staffId: string | null
}

export type AvailabilityStatus = 'AVAILABLE' | 'UNAVAILABLE'

export interface AvailabilityResponse {
  status: AvailabilityStatus
  type: 'APPOINTMENT'
  date: string
  slots: AvailabilitySlot[]
}

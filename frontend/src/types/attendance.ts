export interface AttendanceDto {
  id: string
  businessId: string
  customerId: string
  checkInAt: string
  checkOutAt: string | null
}

/**
 * Check-in. A customer may not have more than one open (checkOutAt: null)
 * attendance record per business at a time — the backend rejects a second
 * check-in with 409.
 */
export interface CreateAttendanceRequest {
  customerId: string
}

export interface AttendanceListParams {
  activeOnly?: boolean
  page?: number
  size?: number
  [key: string]: string | number | boolean | undefined | null
}

/** current/capacity are always derived server-side, never a stored counter. */
export interface CapacityResponse {
  current: number
  capacity: number | null
  available: number | null
}

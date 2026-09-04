import type { BookingStatus } from './enums'

export interface BookingDto {
  id: string
  businessId: string
  customerId: string
  serviceId: string
  staffId: string | null
  startAt: string
  endAt: string
  status: BookingStatus
  price: number
  currency: string
  notes: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateBookingRequest {
  serviceId: string
  staffId?: string | null
  startAt: string
  notes?: string | null
}

export interface CancelBookingRequest {
  reason: string
}

export interface RejectBookingRequest {
  reason: string
}

export interface BookingListParams {
  status?: BookingStatus
  from?: string
  to?: string
  page?: number
  size?: number
  // Index signature so this can be passed straight through to apiClient's query-string
  // builder (which is typed generically over Record<string, QueryValue>).
  [key: string]: string | number | boolean | undefined | null
}

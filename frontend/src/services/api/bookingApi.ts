import type { BookingDto, BookingListParams, CreateBookingRequest, PageResponse } from '../../types'
import { apiClient } from './apiClient'

/** Business-side (owner/staff) booking management. */
export const businessBookingApi = {
  list: (businessId: string, params?: BookingListParams) =>
    apiClient.get<PageResponse<BookingDto>>(`/businesses/${businessId}/bookings`, params),
  confirm: (businessId: string, bookingId: string) =>
    apiClient.post<BookingDto>(`/businesses/${businessId}/bookings/${bookingId}/confirm`),
  checkIn: (businessId: string, bookingId: string) =>
    apiClient.post<BookingDto>(`/businesses/${businessId}/bookings/${bookingId}/check-in`),
  start: (businessId: string, bookingId: string) =>
    apiClient.post<BookingDto>(`/businesses/${businessId}/bookings/${bookingId}/start`),
  complete: (businessId: string, bookingId: string) =>
    apiClient.post<BookingDto>(`/businesses/${businessId}/bookings/${bookingId}/complete`),
  cancel: (businessId: string, bookingId: string, reason: string) =>
    apiClient.post<BookingDto>(`/businesses/${businessId}/bookings/${bookingId}/cancel`, { reason }),
  markNoShow: (businessId: string, bookingId: string) =>
    apiClient.post<BookingDto>(`/businesses/${businessId}/bookings/${bookingId}/no-show`),
  reject: (businessId: string, bookingId: string, reason: string) =>
    apiClient.post<BookingDto>(`/businesses/${businessId}/bookings/${bookingId}/reject`, { reason }),
}

/** Customer-facing bookings — always scoped to the caller (`/me/...`). */
export const customerBookingApi = {
  create: (businessId: string, data: CreateBookingRequest) =>
    apiClient.post<BookingDto>(`/businesses/${businessId}/bookings`, data),
  listMine: (params?: BookingListParams) => apiClient.get<PageResponse<BookingDto>>('/me/bookings', params),
  cancel: (bookingId: string, reason: string) =>
    apiClient.post<BookingDto>(`/me/bookings/${bookingId}/cancel`, { reason }),
}

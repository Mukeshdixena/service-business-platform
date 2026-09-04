import type { CreateQueueEntryRequest, QueueEntryDto, QueueEntryListParams, QueueStatusResponse } from '../../types'
import { apiClient } from './apiClient'

/** Business-side (owner/staff) queue management. */
export const businessQueueApi = {
  list: (businessId: string, params?: QueueEntryListParams) =>
    apiClient.get<QueueEntryDto[]>(`/businesses/${businessId}/queue`, params),
  call: (businessId: string, entryId: string) =>
    apiClient.post<QueueEntryDto>(`/businesses/${businessId}/queue/${entryId}/call`),
  start: (businessId: string, entryId: string) =>
    apiClient.post<QueueEntryDto>(`/businesses/${businessId}/queue/${entryId}/start`),
  complete: (businessId: string, entryId: string) =>
    apiClient.post<QueueEntryDto>(`/businesses/${businessId}/queue/${entryId}/complete`),
  skip: (businessId: string, entryId: string) =>
    apiClient.post<QueueEntryDto>(`/businesses/${businessId}/queue/${entryId}/skip`),
  markNoShow: (businessId: string, entryId: string) =>
    apiClient.post<QueueEntryDto>(`/businesses/${businessId}/queue/${entryId}/no-show`),
  cancel: (businessId: string, entryId: string) =>
    apiClient.post<QueueEntryDto>(`/businesses/${businessId}/queue/${entryId}/cancel`),
}

/** Customer-facing queue actions. Joining/cancelling go through the business-scoped
 *  endpoints (cancel is allowed for "OWNER/STAFF or the owning customer" per the
 *  contract); only the read-your-own views live under /me. */
export const customerQueueApi = {
  join: (businessId: string, data: CreateQueueEntryRequest) =>
    apiClient.post<QueueEntryDto>(`/businesses/${businessId}/queue`, data),
  cancel: (businessId: string, entryId: string) =>
    apiClient.post<QueueEntryDto>(`/businesses/${businessId}/queue/${entryId}/cancel`),
  listMine: () => apiClient.get<QueueEntryDto[]>('/me/queue-entries'),
  getStatus: (entryId: string) => apiClient.get<QueueStatusResponse>(`/me/queue-entries/${entryId}/status`),
}

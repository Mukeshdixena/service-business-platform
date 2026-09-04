import type { AdminBusinessDto, AdminBusinessListParams, PageResponse, PlatformStatsDto } from '../../types'
import { apiClient } from './apiClient'

export const adminApi = {
  getStats: () => apiClient.get<PlatformStatsDto>('/admin/stats'),
  listBusinesses: (params?: AdminBusinessListParams) =>
    apiClient.get<PageResponse<AdminBusinessDto>>('/admin/businesses', params),
  verifyBusiness: (businessId: string, status: string) =>
    apiClient.post<AdminBusinessDto>(`/admin/businesses/${businessId}/verify`, null, { query: { status } }),
  suspendBusiness: (businessId: string) =>
    apiClient.post<AdminBusinessDto>(`/admin/businesses/${businessId}/suspend`),
  activateBusiness: (businessId: string) =>
    apiClient.post<AdminBusinessDto>(`/admin/businesses/${businessId}/activate`),
}

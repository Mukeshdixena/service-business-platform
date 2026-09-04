import type { CreateStaffRequest, PageResponse, StaffDto, UpdateStaffRequest } from '../../types'
import { apiClient } from './apiClient'

export const staffApi = {
  list: (businessId: string, params?: { page?: number; size?: number }) =>
    apiClient.get<PageResponse<StaffDto>>(`/businesses/${businessId}/staff`, params),
  create: (businessId: string, data: CreateStaffRequest) =>
    apiClient.post<StaffDto>(`/businesses/${businessId}/staff`, data),
  update: (businessId: string, staffId: string, data: UpdateStaffRequest) =>
    apiClient.patch<StaffDto>(`/businesses/${businessId}/staff/${staffId}`, data),
  remove: (businessId: string, staffId: string) =>
    apiClient.delete<void>(`/businesses/${businessId}/staff/${staffId}`),
}

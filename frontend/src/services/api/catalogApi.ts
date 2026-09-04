import type { CreateServiceRequest, PageResponse, ServiceDto, UpdateServiceRequest } from '../../types'
import { apiClient } from './apiClient'

/** Named "catalog" (not "service") to avoid clashing with the domain term Service. */
export const catalogApi = {
  list: (businessId: string, params?: { page?: number; size?: number }) =>
    apiClient.get<PageResponse<ServiceDto>>(`/businesses/${businessId}/services`, params),
  create: (businessId: string, data: CreateServiceRequest) =>
    apiClient.post<ServiceDto>(`/businesses/${businessId}/services`, data),
  update: (businessId: string, serviceId: string, data: UpdateServiceRequest) =>
    apiClient.patch<ServiceDto>(`/businesses/${businessId}/services/${serviceId}`, data),
  remove: (businessId: string, serviceId: string) =>
    apiClient.delete<void>(`/businesses/${businessId}/services/${serviceId}`),
}

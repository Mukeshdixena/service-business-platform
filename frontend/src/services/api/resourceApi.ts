import type { CreateResourceRequest, PageResponse, ResourceDto, UpdateResourceRequest } from '../../types'
import { apiClient } from './apiClient'

export const resourceApi = {
  list: (businessId: string, params?: { page?: number; size?: number }) =>
    apiClient.get<PageResponse<ResourceDto>>(`/businesses/${businessId}/resources`, params),
  create: (businessId: string, data: CreateResourceRequest) =>
    apiClient.post<ResourceDto>(`/businesses/${businessId}/resources`, data),
  update: (businessId: string, resourceId: string, data: UpdateResourceRequest) =>
    apiClient.patch<ResourceDto>(`/businesses/${businessId}/resources/${resourceId}`, data),
  remove: (businessId: string, resourceId: string) =>
    apiClient.delete<void>(`/businesses/${businessId}/resources/${resourceId}`),
}

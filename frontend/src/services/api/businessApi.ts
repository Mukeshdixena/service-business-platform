import type {
  BusinessDto,
  BusinessHoursDto,
  CreateBusinessRequest,
  CreateLocationRequest,
  LocationDto,
  UpdateBusinessRequest,
  UpdateLocationRequest,
  UpsertBusinessHoursRequest,
} from '../../types'
import { apiClient } from './apiClient'

export const businessApi = {
  create: (data: CreateBusinessRequest) => apiClient.post<BusinessDto>('/businesses', data),
  get: (businessId: string) => apiClient.get<BusinessDto>(`/businesses/${businessId}`),
  update: (businessId: string, data: UpdateBusinessRequest) =>
    apiClient.patch<BusinessDto>(`/businesses/${businessId}`, data),
  publish: (businessId: string) => apiClient.post<BusinessDto>(`/businesses/${businessId}/publish`),

  listLocations: (businessId: string) => apiClient.get<LocationDto[]>(`/businesses/${businessId}/locations`),
  createLocation: (businessId: string, data: CreateLocationRequest) =>
    apiClient.post<LocationDto>(`/businesses/${businessId}/locations`, data),
  updateLocation: (businessId: string, locationId: string, data: UpdateLocationRequest) =>
    apiClient.patch<LocationDto>(`/businesses/${businessId}/locations/${locationId}`, data),
  deleteLocation: (businessId: string, locationId: string) =>
    apiClient.delete<void>(`/businesses/${businessId}/locations/${locationId}`),

  getHours: (businessId: string) => apiClient.get<BusinessHoursDto[]>(`/businesses/${businessId}/hours`),
  replaceHours: (businessId: string, data: UpsertBusinessHoursRequest) =>
    apiClient.put<BusinessHoursDto[]>(`/businesses/${businessId}/hours`, data),
}

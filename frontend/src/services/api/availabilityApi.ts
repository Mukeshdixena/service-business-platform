import type { AvailabilityResponse } from '../../types'
import { apiClient } from './apiClient'

export interface GetAvailabilityParams {
  serviceId: string
  staffId?: string
  /** "YYYY-MM-DD" */
  date: string
  [key: string]: string | number | boolean | undefined | null
}

export const availabilityApi = {
  get: (businessId: string, params: GetAvailabilityParams) =>
    apiClient.get<AvailabilityResponse>(`/businesses/${businessId}/availability`, params),
}

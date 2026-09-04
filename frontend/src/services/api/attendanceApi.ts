import type { AttendanceDto, AttendanceListParams, CapacityResponse, CreateAttendanceRequest, PageResponse } from '../../types'
import { apiClient } from './apiClient'

/** Business-side (owner/staff) attendance & capacity. */
export const attendanceApi = {
  checkIn: (businessId: string, data: CreateAttendanceRequest) =>
    apiClient.post<AttendanceDto>(`/businesses/${businessId}/attendance/check-in`, data),
  checkOut: (businessId: string, attendanceId: string) =>
    apiClient.post<AttendanceDto>(`/businesses/${businessId}/attendance/${attendanceId}/check-out`),
  list: (businessId: string, params?: AttendanceListParams) =>
    apiClient.get<PageResponse<AttendanceDto>>(`/businesses/${businessId}/attendance`, params),
  getCapacity: (businessId: string) => apiClient.get<CapacityResponse>(`/businesses/${businessId}/capacity`),
}

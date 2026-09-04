import type {
  ClassDto,
  ClassEnrollmentDto,
  ClassListParams,
  CreateClassRequest,
  PageResponse,
  UpdateClassRequest,
} from '../../types'
import { apiClient } from './apiClient'

/** Business-side (owner/staff) class CRUD + roster. */
export const businessClassApi = {
  list: (businessId: string, params?: ClassListParams) =>
    apiClient.get<PageResponse<ClassDto>>(`/businesses/${businessId}/classes`, params),
  create: (businessId: string, data: CreateClassRequest) =>
    apiClient.post<ClassDto>(`/businesses/${businessId}/classes`, data),
  update: (businessId: string, classId: string, data: UpdateClassRequest) =>
    apiClient.patch<ClassDto>(`/businesses/${businessId}/classes/${classId}`, data),
  remove: (businessId: string, classId: string) =>
    apiClient.delete<void>(`/businesses/${businessId}/classes/${classId}`),
  listEnrollments: (businessId: string, classId: string) =>
    apiClient.get<ClassEnrollmentDto[]>(`/businesses/${businessId}/classes/${classId}/enrollments`),
  markAttended: (businessId: string, classId: string, enrollmentId: string) =>
    apiClient.post<ClassEnrollmentDto>(
      `/businesses/${businessId}/classes/${classId}/enrollments/${enrollmentId}/attended`,
    ),
  markNoShow: (businessId: string, classId: string, enrollmentId: string) =>
    apiClient.post<ClassEnrollmentDto>(
      `/businesses/${businessId}/classes/${classId}/enrollments/${enrollmentId}/no-show`,
    ),
}

/** Customer-facing class enrollment actions. */
export const customerClassApi = {
  enroll: (businessId: string, classId: string) =>
    apiClient.post<ClassEnrollmentDto>(`/businesses/${businessId}/classes/${classId}/enroll`),
  cancelEnrollment: (businessId: string, classId: string) =>
    apiClient.post<ClassEnrollmentDto>(`/businesses/${businessId}/classes/${classId}/cancel-enrollment`),
  listMine: () => apiClient.get<ClassEnrollmentDto[]>('/me/class-enrollments'),
}

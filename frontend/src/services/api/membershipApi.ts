import type {
  CreateMembershipPlanRequest,
  CreatePurchaseMembershipRequest,
  MembershipDto,
  MembershipListParams,
  MembershipPlanDto,
  PageResponse,
  UpdateMembershipPlanRequest,
} from '../../types'
import { apiClient } from './apiClient'

/** Business-side (owner) membership plan CRUD. */
export const membershipPlanApi = {
  list: (businessId: string, params?: { page?: number; size?: number }) =>
    apiClient.get<PageResponse<MembershipPlanDto>>(`/businesses/${businessId}/membership-plans`, params),
  create: (businessId: string, data: CreateMembershipPlanRequest) =>
    apiClient.post<MembershipPlanDto>(`/businesses/${businessId}/membership-plans`, data),
  update: (businessId: string, planId: string, data: UpdateMembershipPlanRequest) =>
    apiClient.patch<MembershipPlanDto>(`/businesses/${businessId}/membership-plans/${planId}`, data),
  remove: (businessId: string, planId: string) =>
    apiClient.delete<void>(`/businesses/${businessId}/membership-plans/${planId}`),
}

/** Business-side (owner/staff) purchased-membership management. */
export const businessMembershipApi = {
  list: (businessId: string, params?: MembershipListParams) =>
    apiClient.get<PageResponse<MembershipDto>>(`/businesses/${businessId}/memberships`, params),
  freeze: (businessId: string, membershipId: string) =>
    apiClient.post<MembershipDto>(`/businesses/${businessId}/memberships/${membershipId}/freeze`),
  reactivate: (businessId: string, membershipId: string) =>
    apiClient.post<MembershipDto>(`/businesses/${businessId}/memberships/${membershipId}/reactivate`),
  cancel: (businessId: string, membershipId: string) =>
    apiClient.post<MembershipDto>(`/businesses/${businessId}/memberships/${membershipId}/cancel`),
}

/** Customer-facing membership actions. */
export const customerMembershipApi = {
  purchase: (businessId: string, data: CreatePurchaseMembershipRequest) =>
    apiClient.post<MembershipDto>(`/businesses/${businessId}/memberships`, data),
  cancel: (businessId: string, membershipId: string) =>
    apiClient.post<MembershipDto>(`/businesses/${businessId}/memberships/${membershipId}/cancel`),
  listMine: () => apiClient.get<MembershipDto[]>('/me/memberships'),
}

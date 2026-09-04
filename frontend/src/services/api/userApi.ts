import type { BusinessMembershipDto, CustomerProfileDto, UserDto } from '../../types'
import { apiClient } from './apiClient'

export const userApi = {
  getMe: () => apiClient.get<UserDto>('/users/me'),
  getMyBusinesses: () => apiClient.get<BusinessMembershipDto[]>('/users/me/businesses'),
  getMyCustomerProfile: () => apiClient.get<CustomerProfileDto>('/users/me/customer-profile'),
}

import type { BusinessMembershipRole, PlatformRole } from './enums'

export interface UserDto {
  id: string
  email: string
  fullName: string
  roles: PlatformRole[]
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: UserDto
}

export interface BusinessMembershipDto {
  businessId: string
  businessName: string
  role: BusinessMembershipRole
}

export interface CustomerProfileDto {
  id: string
  userId: string
  phone: string | null
  createdAt: string
}

export interface RegisterRequest {
  email: string
  password: string
  fullName: string
  role: Extract<PlatformRole, 'CUSTOMER' | 'BUSINESS_OWNER'>
}

export interface LoginRequest {
  email: string
  password: string
}

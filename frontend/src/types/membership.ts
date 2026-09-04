import type { MembershipDurationUnit, MembershipPlanStatus, MembershipStatus } from './enums'

export interface MembershipPlanDto {
  id: string
  businessId: string
  name: string
  description: string | null
  price: number
  currency: string
  duration: number
  durationUnit: MembershipDurationUnit
  status: MembershipPlanStatus
}

export type CreateMembershipPlanRequest = Omit<MembershipPlanDto, 'id' | 'businessId' | 'status'>
export type UpdateMembershipPlanRequest = Partial<CreateMembershipPlanRequest> & {
  status?: MembershipPlanStatus
}

export interface MembershipDto {
  id: string
  businessId: string
  customerId: string
  membershipPlanId: string
  startDate: string
  endDate: string
  status: MembershipStatus
  /** Always null this phase — no payment provider is wired up yet. */
  paymentId: string | null
}

export interface CreatePurchaseMembershipRequest {
  membershipPlanId: string
}

export interface MembershipListParams {
  status?: MembershipStatus
  page?: number
  size?: number
  [key: string]: string | number | boolean | undefined | null
}

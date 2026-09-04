import type { StaffStatus } from './enums'

export interface StaffDto {
  id: string
  businessId: string
  userId: string | null
  displayName: string
  title: string | null
  bio: string | null
  imageUrl: string | null
  status: StaffStatus
  serviceIds: string[]
}

/** Same as StaffDto minus `userId` — internal user linkage is never public (§41). */
export type StaffPublicDto = Omit<StaffDto, 'userId'>

export interface CreateStaffRequest {
  displayName: string
  title?: string | null
  bio?: string | null
  imageUrl?: string | null
  serviceIds: string[]
}

export type UpdateStaffRequest = Partial<CreateStaffRequest> & {
  status?: StaffStatus
}

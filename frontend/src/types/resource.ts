import type { ResourceStatus } from './enums'

export interface ResourceDto {
  id: string
  businessId: string
  name: string
  type: string
  description: string | null
  imageUrl: string | null
  identifier: string
  status: ResourceStatus
}

export type CreateResourceRequest = Omit<ResourceDto, 'id' | 'businessId'>
export type UpdateResourceRequest = Partial<CreateResourceRequest> & { status?: ResourceStatus }

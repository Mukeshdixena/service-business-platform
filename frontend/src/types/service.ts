import type { ServiceBookingType, ServiceStatus } from './enums'

export interface ServiceDto {
  id: string
  businessId: string
  name: string
  description: string | null
  price: number
  currency: string
  durationMinutes: number | null
  bookingType: ServiceBookingType
  status: ServiceStatus
  createdAt: string
  updatedAt: string
}

/** Only ever returned for ACTIVE services on ACTIVE businesses; same shape as ServiceDto. */
export type ServicePublicDto = ServiceDto

export interface CreateServiceRequest {
  name: string
  description?: string | null
  price: number
  currency: string
  durationMinutes?: number | null
  bookingType: ServiceBookingType
}

export type UpdateServiceRequest = Partial<CreateServiceRequest> & {
  status?: ServiceStatus
}

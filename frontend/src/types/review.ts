import type { ReviewStatus } from './enums'

export interface ReviewDto {
  id: string
  businessId: string
  customerId: string
  bookingId: string
  rating: number
  comment: string | null
  status: ReviewStatus
  createdAt: string
  updatedAt: string
}

export interface CreateReviewRequest {
  bookingId: string
  rating: number
  comment?: string | null
}

export interface ReviewListParams {
  status?: ReviewStatus
  page?: number
  size?: number
  [key: string]: string | number | boolean | undefined | null
}

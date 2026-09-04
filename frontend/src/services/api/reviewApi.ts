import type { CreateReviewRequest, PageResponse, ReviewDto, ReviewListParams } from '../../types'
import { apiClient } from './apiClient'

export const businessReviewApi = {
  list: (businessId: string, params?: ReviewListParams) =>
    apiClient.get<PageResponse<ReviewDto>>(`/businesses/${businessId}/reviews`, params),
  approve: (businessId: string, reviewId: string) =>
    apiClient.post<ReviewDto>(`/businesses/${businessId}/reviews/${reviewId}/approve`),
  reject: (businessId: string, reviewId: string) =>
    apiClient.post<ReviewDto>(`/businesses/${businessId}/reviews/${reviewId}/reject`),
}

export const customerReviewApi = {
  create: (data: CreateReviewRequest) => apiClient.post<ReviewDto>('/me/reviews', data),
  listMine: (params?: ReviewListParams) => apiClient.get<PageResponse<ReviewDto>>('/me/reviews', params),
}

import type { BusinessPublicDto, BusinessSearchParams, CategoryOption, PageResponse } from '../../types'
import { apiClient } from './apiClient'

export const discoveryApi = {
  listCategories: () => apiClient.get<CategoryOption[]>('/discovery/categories'),
  searchBusinesses: (params: BusinessSearchParams) =>
    apiClient.get<PageResponse<BusinessPublicDto>>('/discovery/businesses', params),
  getBusinessBySlug: (slug: string) => apiClient.get<BusinessPublicDto>(`/discovery/businesses/${slug}`),
}

import type { BusinessCategory } from './enums'

export interface CategoryOption {
  value: BusinessCategory
  label: string
}

export interface BusinessSearchParams {
  query?: string
  category?: BusinessCategory
  city?: string
  page?: number
  size?: number
  // See BookingListParams — lets this pass straight through to apiClient's query builder.
  [key: string]: string | number | boolean | undefined | null
}

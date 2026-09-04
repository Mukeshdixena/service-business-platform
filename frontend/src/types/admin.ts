export interface AdminBusinessDto {
  id: string
  name: string
  slug: string
  category: string
  status: string
  verificationStatus: string
  createdAt: string
}

export interface PlatformStatsDto {
  totalBusinesses: number
  activeBusinesses: number
  totalUsers: number
  totalBookings: number
  pendingReviews: number
}

export interface AdminBusinessListParams {
  status?: string
  page?: number
  size?: number
  [key: string]: string | number | boolean | undefined | null
}

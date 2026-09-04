export interface NotificationDto {
  id: string
  type: string
  title: string
  message: string
  referenceType: string | null
  referenceId: string | null
  isRead: boolean
  createdAt: string
}

export interface NotificationListParams {
  page?: number
  size?: number
  [key: string]: string | number | boolean | undefined | null
}

import type { NotificationDto, NotificationListParams, PageResponse } from '../../types'
import { apiClient } from './apiClient'

export const notificationApi = {
  list: (params?: NotificationListParams) =>
    apiClient.get<PageResponse<NotificationDto>>('/me/notifications', params),
  unreadCount: () => apiClient.get<{ count: number }>('/me/notifications/unread-count'),
  markAllAsRead: () => apiClient.post<void>('/me/notifications/read-all'),
}

import type { NotificationDto } from '../../types'
import { formatDateTime } from '../../utils/date'
import { NotificationTypeBadge } from './NotificationTypeBadge'

export interface NotificationCardProps {
  notification: NotificationDto
}

export function NotificationCard({ notification }: NotificationCardProps) {
  return (
    <div
      className={`flex flex-col gap-2 rounded-lg border p-4 shadow-sm ${
        notification.isRead ? 'border-slate-200 bg-white' : 'border-blue-200 bg-blue-50'
      }`}
    >
      <div className="flex flex-wrap items-center gap-2">
        <NotificationTypeBadge type={notification.type} />
        {!notification.isRead && (
          <span className="h-2 w-2 rounded-full bg-blue-500" aria-label="Unread" />
        )}
      </div>
      <p className="text-sm font-medium text-slate-900">{notification.title}</p>
      <p className="text-sm text-slate-600">{notification.message}</p>
      <p className="text-xs text-slate-400">{formatDateTime(notification.createdAt)}</p>
    </div>
  )
}

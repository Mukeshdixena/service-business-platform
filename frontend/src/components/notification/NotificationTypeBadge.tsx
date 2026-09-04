import { Badge } from '../ui'
import type { BadgeColor } from '../ui'

const TYPE_COLOR: Record<string, BadgeColor> = {
  BOOKING_CONFIRMED: 'green',
  BOOKING_CANCELLED: 'red',
  BOOKING_REMINDER: 'blue',
  QUEUE_POSITION_CHANGED: 'amber',
  QUEUE_NEXT: 'purple',
  QUEUE_READY: 'purple',
  MEMBERSHIP_ACTIVATED: 'green',
  MEMBERSHIP_EXPIRING: 'amber',
  CLASS_REMINDER: 'blue',
  REVIEW_RECEIVED: 'blue',
  REVIEW_APPROVED: 'green',
  REVIEW_REJECTED: 'red',
}

const TYPE_LABEL: Record<string, string> = {
  BOOKING_CONFIRMED: 'Booking confirmed',
  BOOKING_CANCELLED: 'Booking cancelled',
  BOOKING_REMINDER: 'Booking reminder',
  QUEUE_POSITION_CHANGED: 'Queue position changed',
  QUEUE_NEXT: 'You are next',
  QUEUE_READY: 'Your turn is ready',
  MEMBERSHIP_ACTIVATED: 'Membership activated',
  MEMBERSHIP_EXPIRING: 'Membership expiring',
  CLASS_REMINDER: 'Class reminder',
  REVIEW_RECEIVED: 'New review',
  REVIEW_APPROVED: 'Review approved',
  REVIEW_REJECTED: 'Review rejected',
}

export function NotificationTypeBadge({ type }: { type: string }) {
  return (
    <Badge color={TYPE_COLOR[type] ?? 'slate'}>
      {TYPE_LABEL[type] ?? type.replace(/_/g, ' ')}
    </Badge>
  )
}

import { Badge } from '../ui'
import type { BadgeColor } from '../ui'
import type { BookingStatus } from '../../types'

const STATUS_COLOR: Record<BookingStatus, BadgeColor> = {
  PENDING: 'amber',
  CONFIRMED: 'blue',
  CHECKED_IN: 'purple',
  IN_PROGRESS: 'purple',
  COMPLETED: 'green',
  CANCELLED: 'slate',
  NO_SHOW: 'red',
  REJECTED: 'red',
}

const STATUS_LABEL: Record<BookingStatus, string> = {
  PENDING: 'Pending',
  CONFIRMED: 'Confirmed',
  CHECKED_IN: 'Checked in',
  IN_PROGRESS: 'In progress',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
  NO_SHOW: 'No-show',
  REJECTED: 'Rejected',
}

export function BookingStatusBadge({ status }: { status: BookingStatus }) {
  return <Badge color={STATUS_COLOR[status]}>{STATUS_LABEL[status]}</Badge>
}

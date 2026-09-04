import { Badge } from '../ui'
import type { BadgeColor } from '../ui'
import type { QueueEntryStatus } from '../../types'

const STATUS_COLOR: Record<QueueEntryStatus, BadgeColor> = {
  WAITING: 'amber',
  CALLED: 'blue',
  SERVING: 'purple',
  COMPLETED: 'green',
  SKIPPED: 'slate',
  CANCELLED: 'slate',
  NO_SHOW: 'red',
}

const STATUS_LABEL: Record<QueueEntryStatus, string> = {
  WAITING: 'Waiting',
  CALLED: 'Called',
  SERVING: 'Serving',
  COMPLETED: 'Completed',
  SKIPPED: 'Skipped',
  CANCELLED: 'Cancelled',
  NO_SHOW: 'No-show',
}

export function QueueStatusBadge({ status }: { status: QueueEntryStatus }) {
  return <Badge color={STATUS_COLOR[status]}>{STATUS_LABEL[status]}</Badge>
}

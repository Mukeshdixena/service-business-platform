import { Badge } from '../ui'
import type { BadgeColor } from '../ui'
import type { ClassStatus } from '../../types'

const STATUS_COLOR: Record<ClassStatus, BadgeColor> = {
  SCHEDULED: 'blue',
  CANCELLED: 'slate',
  COMPLETED: 'green',
}

const STATUS_LABEL: Record<ClassStatus, string> = {
  SCHEDULED: 'Scheduled',
  CANCELLED: 'Cancelled',
  COMPLETED: 'Completed',
}

export function ClassStatusBadge({ status }: { status: ClassStatus }) {
  return <Badge color={STATUS_COLOR[status]}>{STATUS_LABEL[status]}</Badge>
}

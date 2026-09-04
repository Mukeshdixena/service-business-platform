import { Badge } from '../ui'
import type { BadgeColor } from '../ui'
import type { ClassEnrollmentStatus } from '../../types'

const STATUS_COLOR: Record<ClassEnrollmentStatus, BadgeColor> = {
  ENROLLED: 'green',
  WAITLISTED: 'amber',
  CANCELLED: 'slate',
  ATTENDED: 'blue',
  NO_SHOW: 'red',
}

const STATUS_LABEL: Record<ClassEnrollmentStatus, string> = {
  ENROLLED: 'Enrolled',
  WAITLISTED: 'Waitlisted',
  CANCELLED: 'Cancelled',
  ATTENDED: 'Attended',
  NO_SHOW: 'No-show',
}

export function ClassEnrollmentStatusBadge({ status }: { status: ClassEnrollmentStatus }) {
  return <Badge color={STATUS_COLOR[status]}>{STATUS_LABEL[status]}</Badge>
}

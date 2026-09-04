import { Badge } from '../ui'
import type { BadgeColor } from '../ui'
import type { ReviewStatus } from '../../types'

const STATUS_COLOR: Record<ReviewStatus, BadgeColor> = {
  PENDING: 'amber',
  APPROVED: 'green',
  REJECTED: 'red',
}

const STATUS_LABEL: Record<ReviewStatus, string> = {
  PENDING: 'Pending',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
}

export function ReviewStatusBadge({ status }: { status: ReviewStatus }) {
  return <Badge color={STATUS_COLOR[status]}>{STATUS_LABEL[status]}</Badge>
}

import { Badge } from '../ui'
import type { BadgeColor } from '../ui'
import type { MembershipStatus } from '../../types'

const STATUS_COLOR: Record<MembershipStatus, BadgeColor> = {
  PENDING: 'amber',
  ACTIVE: 'green',
  FROZEN: 'blue',
  EXPIRED: 'slate',
  CANCELLED: 'red',
}

const STATUS_LABEL: Record<MembershipStatus, string> = {
  PENDING: 'Pending',
  ACTIVE: 'Active',
  FROZEN: 'Frozen',
  EXPIRED: 'Expired',
  CANCELLED: 'Cancelled',
}

export function MembershipStatusBadge({ status }: { status: MembershipStatus }) {
  return <Badge color={STATUS_COLOR[status]}>{STATUS_LABEL[status]}</Badge>
}

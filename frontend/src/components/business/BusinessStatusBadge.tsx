import { Badge } from '../ui'
import type { BadgeColor } from '../ui'
import type { BusinessStatus } from '../../types'

const STATUS_COLOR: Record<BusinessStatus, BadgeColor> = {
  DRAFT: 'slate',
  ACTIVE: 'green',
  SUSPENDED: 'amber',
  ARCHIVED: 'red',
}

export function BusinessStatusBadge({ status }: { status: BusinessStatus }) {
  return <Badge color={STATUS_COLOR[status]}>{status}</Badge>
}

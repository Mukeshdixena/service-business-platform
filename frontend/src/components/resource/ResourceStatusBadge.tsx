import { Badge } from '../ui'
import type { BadgeColor } from '../ui'
import type { ResourceStatus } from '../../types'

const STATUS_COLOR: Record<ResourceStatus, BadgeColor> = {
  AVAILABLE: 'green',
  RESERVED: 'blue',
  IN_USE: 'purple',
  MAINTENANCE: 'amber',
  UNAVAILABLE: 'slate',
}

const STATUS_LABEL: Record<ResourceStatus, string> = {
  AVAILABLE: 'Available',
  RESERVED: 'Reserved',
  IN_USE: 'In use',
  MAINTENANCE: 'Maintenance',
  UNAVAILABLE: 'Unavailable',
}

export function ResourceStatusBadge({ status }: { status: ResourceStatus }) {
  return <Badge color={STATUS_COLOR[status]}>{STATUS_LABEL[status]}</Badge>
}

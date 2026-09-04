import type { ReactNode } from 'react'
import type { MembershipDto } from '../../types'
import { formatDate } from '../../utils/date'
import { MembershipStatusBadge } from './MembershipStatusBadge'

export interface MembershipCardProps {
  membership: MembershipDto
  businessLabel?: string
  planLabel?: string
  actions?: ReactNode
}

export function MembershipCard({ membership, businessLabel, planLabel, actions }: MembershipCardProps) {
  return (
    <div className="flex flex-col gap-3 rounded-lg border border-slate-200 bg-white p-4 shadow-sm sm:flex-row sm:items-center sm:justify-between">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="font-medium text-slate-900">
            {planLabel ?? `Plan #${membership.membershipPlanId.slice(0, 8)}`}
          </h3>
          <MembershipStatusBadge status={membership.status} />
        </div>
        {businessLabel && <p className="text-sm text-slate-500">{businessLabel}</p>}
        <p className="mt-1 text-sm text-slate-600">
          {formatDate(membership.startDate)} &ndash; {formatDate(membership.endDate)}
        </p>
      </div>
      {actions && <div className="flex shrink-0 flex-wrap gap-2">{actions}</div>}
    </div>
  )
}

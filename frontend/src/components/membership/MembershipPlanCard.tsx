import type { ReactNode } from 'react'
import { Badge } from '../ui'
import type { MembershipPlanDto } from '../../types'
import { formatMoney } from '../../utils/money'

export interface MembershipPlanCardProps {
  plan: MembershipPlanDto
  action?: ReactNode
}

const DURATION_UNIT_LABEL: Record<MembershipPlanDto['durationUnit'], string> = {
  DAY: 'day',
  WEEK: 'week',
  MONTH: 'month',
  YEAR: 'year',
}

export function MembershipPlanCard({ plan, action }: MembershipPlanCardProps) {
  const unitLabel = DURATION_UNIT_LABEL[plan.durationUnit]
  return (
    <div className="flex flex-col justify-between gap-3 rounded-lg border border-slate-200 bg-white p-4 shadow-sm sm:flex-row sm:items-center">
      <div>
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="font-medium text-slate-900">{plan.name}</h3>
          {plan.status === 'INACTIVE' && <Badge color="slate">Inactive</Badge>}
        </div>
        {plan.description && <p className="mt-1 text-sm text-slate-500">{plan.description}</p>}
        <p className="mt-1 text-sm text-slate-600">
          {formatMoney(plan.price, plan.currency)} / {plan.duration} {unitLabel}
          {plan.duration > 1 ? 's' : ''}
        </p>
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  )
}

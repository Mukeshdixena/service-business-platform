import type { ReactNode } from 'react'
import { Badge } from '../ui'
import type { ServiceDto } from '../../types'
import { formatMoney } from '../../utils/money'

export interface ServiceCardProps {
  service: ServiceDto
  /** Optional slot for context-specific actions — "Book" on the public profile, "Edit/Deactivate" in the dashboard. */
  action?: ReactNode
}

export function ServiceCard({ service, action }: ServiceCardProps) {
  return (
    <div className="flex flex-col justify-between gap-3 rounded-lg border border-slate-200 bg-white p-4 shadow-sm sm:flex-row sm:items-center">
      <div>
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="font-medium text-slate-900">{service.name}</h3>
          {service.status === 'INACTIVE' && <Badge color="slate">Inactive</Badge>}
        </div>
        {service.description && <p className="mt-1 text-sm text-slate-500">{service.description}</p>}
        <p className="mt-1 text-sm text-slate-600">
          {formatMoney(service.price, service.currency)}
          {service.durationMinutes != null && <span> · {service.durationMinutes} min</span>}
        </p>
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  )
}

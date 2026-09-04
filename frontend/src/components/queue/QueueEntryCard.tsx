import type { ReactNode } from 'react'
import type { QueueEntryDto } from '../../types'
import { formatDateTime } from '../../utils/date'
import { QueueStatusBadge } from './QueueStatusBadge'

export interface QueueEntryCardProps {
  entry: QueueEntryDto
  serviceLabel?: string
  staffLabel?: string
  actions?: ReactNode
}

export function QueueEntryCard({ entry, serviceLabel, staffLabel, actions }: QueueEntryCardProps) {
  return (
    <div className="flex flex-col gap-3 rounded-lg border border-slate-200 bg-white p-4 shadow-sm sm:flex-row sm:items-center sm:justify-between">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-slate-100 text-xs font-semibold text-slate-700">
            {entry.position}
          </span>
          <h3 className="font-medium text-slate-900">{serviceLabel ?? `Service #${entry.serviceId.slice(0, 8)}`}</h3>
          <QueueStatusBadge status={entry.status} />
        </div>
        <p className="mt-1 text-sm text-slate-600">Joined {formatDateTime(entry.joinedAt)}</p>
        {staffLabel && <p className="text-sm text-slate-500">with {staffLabel}</p>}
        {entry.estimatedWaitMinutes != null && (
          <p className="mt-1 text-sm text-slate-500">Estimated wait: ~{entry.estimatedWaitMinutes} min</p>
        )}
      </div>
      {actions && <div className="flex shrink-0 flex-wrap gap-2">{actions}</div>}
    </div>
  )
}

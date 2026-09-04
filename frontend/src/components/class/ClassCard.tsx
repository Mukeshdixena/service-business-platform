import type { ReactNode } from 'react'
import { ClassStatusBadge } from './ClassStatusBadge'
import type { ClassDto } from '../../types'
import { formatDateTime } from '../../utils/date'

export interface ClassCardProps {
  classItem: ClassDto
  staffLabel?: string
  action?: ReactNode
}

export function ClassCard({ classItem, staffLabel, action }: ClassCardProps) {
  const isFull = classItem.enrolledCount >= classItem.capacity
  return (
    <div className="flex flex-col justify-between gap-3 rounded-lg border border-slate-200 bg-white p-4 shadow-sm sm:flex-row sm:items-center">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="font-medium text-slate-900">{classItem.name}</h3>
          <ClassStatusBadge status={classItem.status} />
        </div>
        {classItem.description && <p className="mt-1 text-sm text-slate-500">{classItem.description}</p>}
        <p className="mt-1 text-sm text-slate-600">{formatDateTime(classItem.startAt)}</p>
        {staffLabel && <p className="text-sm text-slate-500">with {staffLabel}</p>}
        <p className={`mt-1 text-sm font-medium ${isFull ? 'text-amber-700' : 'text-slate-700'}`}>
          {classItem.enrolledCount}/{classItem.capacity} enrolled{isFull ? ' · full' : ''}
        </p>
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  )
}

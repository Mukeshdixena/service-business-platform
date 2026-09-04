import type { ReactNode } from 'react'
import { Badge } from '../ui'
import type { StaffDto, StaffPublicDto } from '../../types'

export interface StaffCardProps {
  staff: StaffDto | StaffPublicDto
  action?: ReactNode
}

export function StaffCard({ staff, action }: StaffCardProps) {
  return (
    <div className="flex items-center gap-3 rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <div className="h-12 w-12 shrink-0 overflow-hidden rounded-full bg-slate-100">
        {staff.imageUrl ? (
          <img src={staff.imageUrl} alt={staff.displayName} className="h-full w-full object-cover" />
        ) : (
          <div className="flex h-full w-full items-center justify-center text-sm font-semibold text-slate-400">
            {staff.displayName.charAt(0).toUpperCase()}
          </div>
        )}
      </div>
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <p className="font-medium text-slate-900">{staff.displayName}</p>
          {staff.status === 'INACTIVE' && <Badge color="slate">Inactive</Badge>}
        </div>
        {staff.title && <p className="text-sm text-slate-500">{staff.title}</p>}
        {staff.bio && <p className="mt-1 line-clamp-2 text-sm text-slate-500">{staff.bio}</p>}
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  )
}

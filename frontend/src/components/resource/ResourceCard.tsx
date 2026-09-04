import type { ReactNode } from 'react'
import { ResourceStatusBadge } from './ResourceStatusBadge'
import type { ResourceDto } from '../../types'

export interface ResourceCardProps {
  resource: ResourceDto
  action?: ReactNode
}

export function ResourceCard({ resource, action }: ResourceCardProps) {
  return (
    <div className="flex flex-col justify-between gap-3 rounded-lg border border-slate-200 bg-white p-4 shadow-sm sm:flex-row sm:items-center">
      <div>
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="font-medium text-slate-900">{resource.name}</h3>
          <ResourceStatusBadge status={resource.status} />
        </div>
        <p className="mt-1 text-sm text-slate-600">
          {resource.type} · {resource.identifier}
        </p>
        {resource.description && <p className="mt-1 text-sm text-slate-500">{resource.description}</p>}
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  )
}

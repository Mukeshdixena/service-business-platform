import type { ReactNode } from 'react'
import type { BusinessPublicDto } from '../../types'
import { CATEGORY_LABELS } from '../../utils/categoryLabels'

export interface BusinessHeaderProps {
  business: BusinessPublicDto
  action?: ReactNode
}

/** Presentational hero for a business's public profile page. */
export function BusinessHeader({ business, action }: BusinessHeaderProps) {
  const primaryLocation = business.locations?.find((location) => location.isPrimary) ?? business.locations?.[0]

  return (
    <div className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
      <div className="h-32 w-full bg-gradient-to-r from-brand-600 to-brand-400 sm:h-40">
        {business.coverImageUrl && (
          <img src={business.coverImageUrl} alt="" className="h-full w-full object-cover" />
        )}
      </div>
      <div className="flex flex-col gap-4 px-4 py-4 sm:flex-row sm:items-end sm:justify-between sm:px-6">
        <div className="flex items-start gap-4">
          <div className="-mt-10 h-20 w-20 shrink-0 overflow-hidden rounded-lg border-4 border-white bg-slate-100 shadow sm:-mt-14 sm:h-24 sm:w-24">
            {business.logoUrl ? (
              <img src={business.logoUrl} alt={`${business.name} logo`} className="h-full w-full object-cover" />
            ) : (
              <div className="flex h-full w-full items-center justify-center text-2xl font-semibold text-slate-400">
                {business.name.charAt(0).toUpperCase()}
              </div>
            )}
          </div>
          <div className="pt-2">
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="text-xl font-semibold text-slate-900">{business.name}</h1>
              <span className="rounded-full bg-brand-50 px-2.5 py-0.5 text-xs font-medium text-brand-700">
                {CATEGORY_LABELS[business.category]}
              </span>
            </div>
            {primaryLocation && (
              <p className="mt-1 text-sm text-slate-500">
                {primaryLocation.city}, {primaryLocation.state}
              </p>
            )}
            {business.description && (
              <p className="mt-2 max-w-2xl text-sm text-slate-600">{business.description}</p>
            )}
          </div>
        </div>
        {action && <div className="shrink-0">{action}</div>}
      </div>
    </div>
  )
}

import { Link } from 'react-router-dom'
import type { BusinessPublicDto } from '../../types'
import { CATEGORY_LABELS } from '../../utils/categoryLabels'

export interface BusinessCardProps {
  business: BusinessPublicDto
}

/** Summary card for a discovery search result. */
export function BusinessCard({ business }: BusinessCardProps) {
  const primaryLocation = business.locations?.find((location) => location.isPrimary) ?? business.locations?.[0]

  return (
    <Link
      to={`/businesses/${business.slug}`}
      className="flex gap-4 rounded-lg border border-slate-200 bg-white p-4 shadow-sm transition-shadow hover:shadow-md"
    >
      <div className="h-14 w-14 shrink-0 overflow-hidden rounded-lg bg-slate-100">
        {business.logoUrl ? (
          <img src={business.logoUrl} alt="" className="h-full w-full object-cover" />
        ) : (
          <div className="flex h-full w-full items-center justify-center text-lg font-semibold text-slate-400">
            {business.name.charAt(0).toUpperCase()}
          </div>
        )}
      </div>
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="truncate font-medium text-slate-900">{business.name}</h3>
          <span className="shrink-0 rounded-full bg-brand-50 px-2 py-0.5 text-xs font-medium text-brand-700">
            {CATEGORY_LABELS[business.category]}
          </span>
        </div>
        {primaryLocation && (
          <p className="mt-0.5 text-sm text-slate-500">
            {primaryLocation.city}
            {primaryLocation.state ? `, ${primaryLocation.state}` : ''}
          </p>
        )}
        {business.description && <p className="mt-1 line-clamp-2 text-sm text-slate-500">{business.description}</p>}
      </div>
    </Link>
  )
}

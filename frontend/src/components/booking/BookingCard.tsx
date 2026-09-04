import type { ReactNode } from 'react'
import type { BookingDto } from '../../types'
import { formatDateTime } from '../../utils/date'
import { formatMoney } from '../../utils/money'
import { BookingStatusBadge } from './BookingStatusBadge'

export interface BookingCardProps {
  booking: BookingDto
  /** Human labels resolved by the caller (BookingDto only carries IDs) — see hooks/useEntityLookup. */
  businessLabel?: string
  serviceLabel?: string
  staffLabel?: string
  actions?: ReactNode
}

export function BookingCard({ booking, businessLabel, serviceLabel, staffLabel, actions }: BookingCardProps) {
  return (
    <div className="flex flex-col gap-3 rounded-lg border border-slate-200 bg-white p-4 shadow-sm sm:flex-row sm:items-center sm:justify-between">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="font-medium text-slate-900">
            {serviceLabel ?? `Service #${booking.serviceId.slice(0, 8)}`}
          </h3>
          <BookingStatusBadge status={booking.status} />
        </div>
        {businessLabel && <p className="text-sm text-slate-500">{businessLabel}</p>}
        <p className="mt-1 text-sm text-slate-600">{formatDateTime(booking.startAt)}</p>
        {staffLabel && <p className="text-sm text-slate-500">with {staffLabel}</p>}
        <p className="mt-1 text-sm font-medium text-slate-700">{formatMoney(booking.price, booking.currency)}</p>
        {booking.notes && <p className="mt-1 text-sm italic text-slate-500">&ldquo;{booking.notes}&rdquo;</p>}
      </div>
      {actions && <div className="flex shrink-0 flex-wrap gap-2">{actions}</div>}
    </div>
  )
}

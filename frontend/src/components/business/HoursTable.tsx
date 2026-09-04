import type { BusinessHoursDto, DayOfWeek } from '../../types'
import { DAYS_OF_WEEK } from '../../types'
import { formatHourMinute } from '../../utils/date'
import { EmptyState } from '../ui'

const DAY_LABELS: Record<DayOfWeek, string> = {
  MONDAY: 'Monday',
  TUESDAY: 'Tuesday',
  WEDNESDAY: 'Wednesday',
  THURSDAY: 'Thursday',
  FRIDAY: 'Friday',
  SATURDAY: 'Saturday',
  SUNDAY: 'Sunday',
}

export interface HoursTableProps {
  hours: BusinessHoursDto[]
}

/** Read-only display of business hours, grouped by day with support for multiple intervals/day (§12). */
export function HoursTable({ hours }: HoursTableProps) {
  if (hours.length === 0) {
    return <EmptyState title="Hours not set" description="This business hasn't published its opening hours yet." />
  }

  return (
    <dl className="divide-y divide-slate-100">
      {DAYS_OF_WEEK.map((day) => {
        const intervals = hours
          .filter((row) => row.dayOfWeek === day)
          .sort((a, b) => a.openTime.localeCompare(b.openTime))
        return (
          <div key={day} className="flex items-center justify-between gap-4 py-2 text-sm">
            <dt className="font-medium text-slate-700">{DAY_LABELS[day]}</dt>
            <dd className="text-right text-slate-500">
              {intervals.length === 0
                ? 'Closed'
                : intervals
                    .map((interval) => `${formatHourMinute(interval.openTime)} – ${formatHourMinute(interval.closeTime)}`)
                    .join(', ')}
            </dd>
          </div>
        )
      })}
    </dl>
  )
}

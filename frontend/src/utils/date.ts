/** Formats an ISO-8601 UTC instant as a short local date, e.g. "10 Sep 2026". */
export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
}

/** Formats an ISO-8601 UTC instant as a local time, e.g. "2:30 PM". */
export function formatTime(iso: string): string {
  return new Date(iso).toLocaleTimeString(undefined, {
    hour: 'numeric',
    minute: '2-digit',
  })
}

/** Formats an ISO-8601 UTC instant as local date + time. */
export function formatDateTime(iso: string): string {
  return `${formatDate(iso)}, ${formatTime(iso)}`
}

/** Returns today's date as "YYYY-MM-DD" in the local timezone (for date pickers / query params). */
export function todayIsoDate(): string {
  return toIsoDate(new Date())
}

export function toIsoDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function addDays(isoDate: string, days: number): string {
  const parts = isoDate.split('-').map(Number)
  const year = parts[0] ?? 0
  const month = parts[1] ?? 1
  const day = parts[2] ?? 1
  const date = new Date(year, month - 1, day)
  date.setDate(date.getDate() + days)
  return toIsoDate(date)
}

/** Human label for a "YYYY-MM-DD" date string, e.g. "Thursday, 10 Sep 2026". */
export function formatIsoDateLong(isoDate: string): string {
  const parts = isoDate.split('-').map(Number)
  const year = parts[0] ?? 0
  const month = parts[1] ?? 1
  const day = parts[2] ?? 1
  const date = new Date(year, month - 1, day)
  return date.toLocaleDateString(undefined, {
    weekday: 'long',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
}

/** "HH:mm" (24h) -> "2:30 PM" style label, for BusinessHoursDto rows. */
export function formatHourMinute(hhmm: string): string {
  const parts = hhmm.split(':').map(Number)
  const hours = parts[0] ?? 0
  const minutes = parts[1] ?? 0
  const date = new Date(2000, 0, 1, hours, minutes)
  return date.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' })
}

import type { PricingUnit } from '../types'

const MS_PER_HOUR = 60 * 60 * 1000
const MS_PER_DAY = 24 * MS_PER_HOUR

/**
 * Client-side *estimate* only, for display before submitting a rental booking
 * — the server is always the source of truth for the actual charged price
 * (API_CONTRACT.md: "a rental booking's total price = rate × ceil(duration /
 * unit), computed server-side"). Mirrors that formula so the UI can show a
 * sensible number while the customer is still picking a duration.
 *
 * Returns null for a non-positive or invalid duration (e.g. endAt <= startAt).
 */
export function estimateRentalPrice(rate: number, pricingUnit: PricingUnit, startAt: Date, endAt: Date): number | null {
  const durationMs = endAt.getTime() - startAt.getTime()
  if (!Number.isFinite(durationMs) || durationMs <= 0) return null

  const unitMs = pricingUnit === 'HOUR' ? MS_PER_HOUR : MS_PER_DAY
  const units = Math.ceil(durationMs / unitMs)
  return Math.round(rate * units * 100) / 100
}

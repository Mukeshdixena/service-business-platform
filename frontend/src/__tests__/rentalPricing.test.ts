import { describe, expect, it } from 'vitest'
import { estimateRentalPrice } from '../utils/rentalPricing'

describe('estimateRentalPrice', () => {
  it('charges a whole number of hours, rounding partial hours up', () => {
    const start = new Date('2026-09-10T09:00:00Z')
    const end = new Date('2026-09-10T11:30:00Z') // 2.5 hours -> rounds up to 3
    expect(estimateRentalPrice(100, 'HOUR', start, end)).toBe(300)
  })

  it('charges exactly for a whole number of hours with no rounding needed', () => {
    const start = new Date('2026-09-10T09:00:00Z')
    const end = new Date('2026-09-10T12:00:00Z') // exactly 3 hours
    expect(estimateRentalPrice(50, 'HOUR', start, end)).toBe(150)
  })

  it('charges a whole number of days, rounding partial days up', () => {
    const start = new Date('2026-09-10T09:00:00Z')
    const end = new Date('2026-09-12T10:00:00Z') // just over 2 days -> rounds up to 3
    expect(estimateRentalPrice(1000, 'DAY', start, end)).toBe(3000)
  })

  it('returns null for a non-positive duration (endAt <= startAt)', () => {
    const start = new Date('2026-09-10T09:00:00Z')
    expect(estimateRentalPrice(100, 'HOUR', start, start)).toBeNull()
    expect(estimateRentalPrice(100, 'HOUR', start, new Date('2026-09-10T08:00:00Z'))).toBeNull()
  })
})

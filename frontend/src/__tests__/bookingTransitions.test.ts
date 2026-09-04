import { describe, expect, it } from 'vitest'
import { customerCanCancel, getAvailableBookingActions } from '../utils/bookingTransitions'

describe('bookingTransitions', () => {
  it('only offers actions allowed by the server-enforced state machine', () => {
    expect(getAvailableBookingActions('PENDING').map((a) => a.action)).toEqual(['confirm', 'reject', 'cancel'])
    expect(getAvailableBookingActions('CONFIRMED').map((a) => a.action)).toEqual(['check-in', 'cancel', 'no-show'])
    expect(getAvailableBookingActions('CHECKED_IN').map((a) => a.action)).toEqual(['start', 'cancel'])
    expect(getAvailableBookingActions('IN_PROGRESS').map((a) => a.action)).toEqual(['complete'])
  })

  it('offers no actions for terminal statuses', () => {
    expect(getAvailableBookingActions('COMPLETED')).toEqual([])
    expect(getAvailableBookingActions('CANCELLED')).toEqual([])
    expect(getAvailableBookingActions('NO_SHOW')).toEqual([])
    expect(getAvailableBookingActions('REJECTED')).toEqual([])
  })

  it('flags cancel/reject as requiring a reason, matching the API contract', () => {
    const pendingActions = getAvailableBookingActions('PENDING')
    const cancel = pendingActions.find((a) => a.action === 'cancel')
    const reject = pendingActions.find((a) => a.action === 'reject')
    const confirm = pendingActions.find((a) => a.action === 'confirm')

    expect(cancel?.requiresReason).toBe(true)
    expect(reject?.requiresReason).toBe(true)
    expect(confirm?.requiresReason).toBe(false)
  })

  it('only lets the customer cancel while PENDING or CONFIRMED', () => {
    expect(customerCanCancel('PENDING')).toBe(true)
    expect(customerCanCancel('CONFIRMED')).toBe(true)
    expect(customerCanCancel('CHECKED_IN')).toBe(false)
    expect(customerCanCancel('COMPLETED')).toBe(false)
  })
})

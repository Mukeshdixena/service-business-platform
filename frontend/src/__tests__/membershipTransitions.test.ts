import { describe, expect, it } from 'vitest'
import { customerCanCancelMembership, getAvailableMembershipActions } from '../utils/membershipTransitions'

describe('membershipTransitions', () => {
  it('only offers actions allowed by the server-enforced state machine', () => {
    expect(getAvailableMembershipActions('PENDING').map((a) => a.action)).toEqual(['cancel'])
    expect(getAvailableMembershipActions('ACTIVE').map((a) => a.action)).toEqual(['freeze', 'cancel'])
    expect(getAvailableMembershipActions('FROZEN').map((a) => a.action)).toEqual(['reactivate', 'cancel'])
  })

  it('offers no actions for terminal statuses', () => {
    expect(getAvailableMembershipActions('EXPIRED')).toEqual([])
    expect(getAvailableMembershipActions('CANCELLED')).toEqual([])
  })

  it('lets the customer cancel while PENDING, ACTIVE, or FROZEN, but not once terminal', () => {
    expect(customerCanCancelMembership('PENDING')).toBe(true)
    expect(customerCanCancelMembership('ACTIVE')).toBe(true)
    expect(customerCanCancelMembership('FROZEN')).toBe(true)
    expect(customerCanCancelMembership('EXPIRED')).toBe(false)
    expect(customerCanCancelMembership('CANCELLED')).toBe(false)
  })
})

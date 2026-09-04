import { describe, expect, it } from 'vitest'
import { customerCanCancelQueueEntry, getAvailableQueueActions, isActiveQueueStatus } from '../utils/queueTransitions'

describe('queueTransitions', () => {
  it('only offers actions allowed by the server-enforced state machine', () => {
    expect(getAvailableQueueActions('WAITING').map((a) => a.action)).toEqual(['call', 'skip', 'cancel'])
    expect(getAvailableQueueActions('CALLED').map((a) => a.action)).toEqual(['start', 'no-show', 'cancel'])
    expect(getAvailableQueueActions('SERVING').map((a) => a.action)).toEqual(['complete'])
  })

  it('offers no actions for terminal statuses', () => {
    expect(getAvailableQueueActions('COMPLETED')).toEqual([])
    expect(getAvailableQueueActions('SKIPPED')).toEqual([])
    expect(getAvailableQueueActions('CANCELLED')).toEqual([])
    expect(getAvailableQueueActions('NO_SHOW')).toEqual([])
  })

  it('only lets the customer cancel while WAITING or CALLED', () => {
    expect(customerCanCancelQueueEntry('WAITING')).toBe(true)
    expect(customerCanCancelQueueEntry('CALLED')).toBe(true)
    expect(customerCanCancelQueueEntry('SERVING')).toBe(false)
    expect(customerCanCancelQueueEntry('COMPLETED')).toBe(false)
  })

  it('treats WAITING/CALLED/SERVING as active, everything else as terminal', () => {
    expect(isActiveQueueStatus('WAITING')).toBe(true)
    expect(isActiveQueueStatus('CALLED')).toBe(true)
    expect(isActiveQueueStatus('SERVING')).toBe(true)
    expect(isActiveQueueStatus('COMPLETED')).toBe(false)
    expect(isActiveQueueStatus('SKIPPED')).toBe(false)
    expect(isActiveQueueStatus('CANCELLED')).toBe(false)
    expect(isActiveQueueStatus('NO_SHOW')).toBe(false)
  })
})

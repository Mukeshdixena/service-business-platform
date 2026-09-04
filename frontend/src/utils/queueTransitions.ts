import type { QueueEntryStatus } from '../types'

/**
 * The six explicit lifecycle actions the backend exposes as endpoints
 * (POST .../queue/{id}/{action}). This is the only vocabulary the dashboard
 * is allowed to use to change a queue entry's status (API_CONTRACT.md queue
 * state machine, mirrors utils/bookingTransitions.ts's pattern).
 */
export const QUEUE_ACTIONS = ['call', 'start', 'complete', 'skip', 'no-show', 'cancel'] as const
export type QueueAction = (typeof QUEUE_ACTIONS)[number]

interface QueueActionConfig {
  action: QueueAction
  label: string
  tone: 'primary' | 'danger' | 'neutral'
}

const ACTION_CONFIG: Record<QueueAction, QueueActionConfig> = {
  call: { action: 'call', label: 'Call', tone: 'primary' },
  start: { action: 'start', label: 'Start serving', tone: 'primary' },
  complete: { action: 'complete', label: 'Complete', tone: 'primary' },
  skip: { action: 'skip', label: 'Skip', tone: 'neutral' },
  'no-show': { action: 'no-show', label: 'Mark no-show', tone: 'neutral' },
  cancel: { action: 'cancel', label: 'Cancel', tone: 'danger' },
}

/**
 * Server-enforced state machine (API_CONTRACT.md), mirrored here purely so the
 * dashboard only ever *offers* legal actions. The backend re-validates every
 * transition regardless.
 *
 * WAITING -> CALLED, SKIPPED, CANCELLED
 * CALLED  -> SERVING, NO_SHOW, CANCELLED
 * SERVING -> COMPLETED
 */
const ALLOWED_ACTIONS_BY_STATUS: Record<QueueEntryStatus, QueueAction[]> = {
  WAITING: ['call', 'skip', 'cancel'],
  CALLED: ['start', 'no-show', 'cancel'],
  SERVING: ['complete'],
  COMPLETED: [],
  SKIPPED: [],
  CANCELLED: [],
  NO_SHOW: [],
}

/** Actions the business dashboard may offer for a queue entry in the given status. */
export function getAvailableQueueActions(status: QueueEntryStatus): QueueActionConfig[] {
  return ALLOWED_ACTIONS_BY_STATUS[status].map((action) => ACTION_CONFIG[action])
}

/** Whether the *customer* (not the business) may cancel a queue entry in this status. */
export function customerCanCancelQueueEntry(status: QueueEntryStatus): boolean {
  return status === 'WAITING' || status === 'CALLED'
}

/** Whether a queue entry is still "in flight" (worth polling / showing in an active queue view). */
export function isActiveQueueStatus(status: QueueEntryStatus): boolean {
  return status === 'WAITING' || status === 'CALLED' || status === 'SERVING'
}

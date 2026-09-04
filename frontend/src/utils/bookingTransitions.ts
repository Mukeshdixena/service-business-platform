import type { BookingStatus } from '../types'

/**
 * The seven explicit lifecycle actions the backend exposes as endpoints
 * (POST .../bookings/{id}/{action}). This is the *only* vocabulary the UI is
 * allowed to use to change a booking's status — never an arbitrary PATCH of
 * `status` (CLAUDE_CODE.md §14, API_CONTRACT.md booking state machine).
 */
export const BOOKING_ACTIONS = [
  'confirm',
  'check-in',
  'start',
  'complete',
  'cancel',
  'no-show',
  'reject',
] as const
export type BookingAction = (typeof BOOKING_ACTIONS)[number]

interface BookingActionConfig {
  action: BookingAction
  label: string
  /** True for actions that call POST .../{action} with a `{ reason }` body. */
  requiresReason: boolean
  /** Visual intent for the action button. */
  tone: 'primary' | 'danger' | 'neutral'
}

const ACTION_CONFIG: Record<BookingAction, BookingActionConfig> = {
  confirm: { action: 'confirm', label: 'Confirm', requiresReason: false, tone: 'primary' },
  'check-in': { action: 'check-in', label: 'Check in', requiresReason: false, tone: 'primary' },
  start: { action: 'start', label: 'Start', requiresReason: false, tone: 'primary' },
  complete: { action: 'complete', label: 'Complete', requiresReason: false, tone: 'primary' },
  cancel: { action: 'cancel', label: 'Cancel', requiresReason: true, tone: 'danger' },
  'no-show': { action: 'no-show', label: 'Mark no-show', requiresReason: false, tone: 'neutral' },
  reject: { action: 'reject', label: 'Reject', requiresReason: true, tone: 'danger' },
}

/**
 * Server-enforced state machine (API_CONTRACT.md), mirrored here purely so the
 * dashboard only ever *offers* legal actions. The backend re-validates every
 * transition regardless — this is UX, not the authorization boundary.
 *
 * PENDING     -> CONFIRMED, REJECTED, CANCELLED
 * CONFIRMED   -> CHECKED_IN, CANCELLED, NO_SHOW
 * CHECKED_IN  -> IN_PROGRESS, CANCELLED
 * IN_PROGRESS -> COMPLETED
 */
const ALLOWED_ACTIONS_BY_STATUS: Record<BookingStatus, BookingAction[]> = {
  PENDING: ['confirm', 'reject', 'cancel'],
  CONFIRMED: ['check-in', 'cancel', 'no-show'],
  CHECKED_IN: ['start', 'cancel'],
  IN_PROGRESS: ['complete'],
  COMPLETED: [],
  CANCELLED: [],
  NO_SHOW: [],
  REJECTED: [],
}

/** Actions the business dashboard may offer for a booking in the given status. */
export function getAvailableBookingActions(status: BookingStatus): BookingActionConfig[] {
  return ALLOWED_ACTIONS_BY_STATUS[status].map((action) => ACTION_CONFIG[action])
}

/** Whether the *customer* (not the business) may cancel a booking in this status. */
export function customerCanCancel(status: BookingStatus): boolean {
  return status === 'PENDING' || status === 'CONFIRMED'
}

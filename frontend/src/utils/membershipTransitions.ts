import type { MembershipStatus } from '../types'

/**
 * The three explicit lifecycle actions the backend exposes as endpoints
 * (POST .../memberships/{id}/{action}). Mirrors utils/bookingTransitions.ts's
 * pattern — this is the only vocabulary the UI uses to change a membership's
 * status, never an arbitrary PATCH.
 */
export const MEMBERSHIP_ACTIONS = ['freeze', 'reactivate', 'cancel'] as const
export type MembershipAction = (typeof MEMBERSHIP_ACTIONS)[number]

interface MembershipActionConfig {
  action: MembershipAction
  label: string
  tone: 'primary' | 'danger' | 'neutral'
}

const ACTION_CONFIG: Record<MembershipAction, MembershipActionConfig> = {
  freeze: { action: 'freeze', label: 'Freeze', tone: 'neutral' },
  reactivate: { action: 'reactivate', label: 'Reactivate', tone: 'primary' },
  cancel: { action: 'cancel', label: 'Cancel', tone: 'danger' },
}

/**
 * Server-enforced state machine (API_CONTRACT.md), mirrored here purely so the
 * UI only ever *offers* legal actions.
 *
 * PENDING  -> ACTIVE, CANCELLED   (ACTIVE is reached automatically on purchase — no client action)
 * ACTIVE   -> FROZEN, CANCELLED, EXPIRED   (EXPIRED happens automatically, not via a client action)
 * FROZEN   -> ACTIVE, CANCELLED
 */
const ALLOWED_ACTIONS_BY_STATUS: Record<MembershipStatus, MembershipAction[]> = {
  PENDING: ['cancel'],
  ACTIVE: ['freeze', 'cancel'],
  FROZEN: ['reactivate', 'cancel'],
  EXPIRED: [],
  CANCELLED: [],
}

/** Actions the business dashboard may offer for a membership in the given status. */
export function getAvailableMembershipActions(status: MembershipStatus): MembershipActionConfig[] {
  return ALLOWED_ACTIONS_BY_STATUS[status].map((action) => ACTION_CONFIG[action])
}

/** Whether the *customer* (not the business) may cancel a membership in this status. */
export function customerCanCancelMembership(status: MembershipStatus): boolean {
  return status === 'PENDING' || status === 'ACTIVE' || status === 'FROZEN'
}

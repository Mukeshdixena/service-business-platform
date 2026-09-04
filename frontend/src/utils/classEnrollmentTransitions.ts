import type { ClassEnrollmentStatus } from '../types'

/**
 * Class enrollments don't have a documented general state-machine transition
 * table like bookings/queue/memberships do — API_CONTRACT.md only exposes
 * three explicit actions: the roster `attended`/`no-show` actions (OWNER/STAFF,
 * ENROLLED only) and the customer's own `cancel-enrollment`. Mirrors the
 * pattern of utils/queueTransitions.ts / utils/membershipTransitions.ts so the
 * UI only ever *offers* legal actions; the backend re-validates regardless.
 */
export const CLASS_ROSTER_ACTIONS = ['attended', 'no-show'] as const
export type ClassRosterAction = (typeof CLASS_ROSTER_ACTIONS)[number]

interface ClassRosterActionConfig {
  action: ClassRosterAction
  label: string
  tone: 'primary' | 'danger' | 'neutral'
}

const ROSTER_ACTION_CONFIG: Record<ClassRosterAction, ClassRosterActionConfig> = {
  attended: { action: 'attended', label: 'Mark attended', tone: 'primary' },
  'no-show': { action: 'no-show', label: 'Mark no-show', tone: 'neutral' },
}

/** Roster actions the business dashboard may offer for an enrollment in the given status. */
export function getAvailableRosterActions(status: ClassEnrollmentStatus): ClassRosterActionConfig[] {
  return status === 'ENROLLED' ? [ROSTER_ACTION_CONFIG.attended, ROSTER_ACTION_CONFIG['no-show']] : []
}

/** Whether the *customer* may cancel their own enrollment in this status. */
export function customerCanCancelEnrollment(status: ClassEnrollmentStatus): boolean {
  return status === 'ENROLLED' || status === 'WAITLISTED'
}

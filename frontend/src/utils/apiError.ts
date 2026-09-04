import type { ApiErrorBody, ErrorCode } from '../types'

/**
 * Thrown by apiClient for every non-2xx response. Carries the parsed backend
 * error body so callers/UI can branch on `code` when needed (e.g. surfacing
 * field-level validation errors) while still having a ready-to-display message.
 */
export class ApiError extends Error {
  readonly status: number
  readonly code: ErrorCode | string
  readonly path: string

  constructor(body: ApiErrorBody) {
    super(body.message)
    this.name = 'ApiError'
    this.status = body.status
    this.code = body.code
    this.path = body.path
  }
}

/**
 * Single shared place mapping backend error codes to user-friendly copy, per
 * CLAUDE_CODE.md §47. UI components should call this instead of showing
 * `error.message` directly, since the backend message is not guaranteed to be
 * end-user phrasing for every code.
 */
const ERROR_MESSAGES: Record<ErrorCode, string> = {
  VALIDATION_ERROR: 'Some of the information provided is invalid. Please check the highlighted fields.',
  UNAUTHENTICATED: 'Your session has expired. Please sign in again.',
  FORBIDDEN: "You don't have permission to do that.",
  NOT_FOUND: "We couldn't find what you were looking for.",
  DUPLICATE_SLUG: 'That name is already taken. Please choose another.',
  BOOKING_CONFLICT: 'That time slot is no longer available. Please pick another.',
  QUEUE_CONFLICT: "You're already in this business's queue.",
  INVALID_STATE_TRANSITION: 'This action is no longer possible for the current status.',
  INTERNAL_ERROR: 'Something went wrong on our end. Please try again shortly.',
}

const FALLBACK_MESSAGE = 'Something went wrong. Please try again.'

export function getFriendlyErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    const known = ERROR_MESSAGES[error.code as ErrorCode]
    // Prefer the backend's message for VALIDATION_ERROR since it usually names
    // the offending field; fall back to our generic copy for everything else
    // (backend messages for other codes are written for logs, not end users).
    if (error.code === 'VALIDATION_ERROR' && error.message) return error.message
    return known ?? error.message ?? FALLBACK_MESSAGE
  }
  if (error instanceof Error && error.message === 'NETWORK_ERROR') {
    return "Can't reach the server. Check your connection and try again."
  }
  return FALLBACK_MESSAGE
}

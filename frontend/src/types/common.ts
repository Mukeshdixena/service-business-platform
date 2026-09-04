/** Envelope every list endpoint returns (API_CONTRACT.md "Conventions"). */
export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
}

/** `code` values from API_CONTRACT.md "## Error format". */
export const ERROR_CODES = [
  'VALIDATION_ERROR',
  'UNAUTHENTICATED',
  'FORBIDDEN',
  'NOT_FOUND',
  'DUPLICATE_SLUG',
  'BOOKING_CONFLICT',
  'QUEUE_CONFLICT',
  'INVALID_STATE_TRANSITION',
  'INTERNAL_ERROR',
] as const
export type ErrorCode = (typeof ERROR_CODES)[number]

/** Shape of every non-2xx response body. */
export interface ApiErrorBody {
  timestamp: string
  status: number
  code: ErrorCode | string
  message: string
  path: string
}

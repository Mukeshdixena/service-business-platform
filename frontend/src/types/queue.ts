import type { QueueEntryStatus } from './enums'

export interface QueueEntryDto {
  id: string
  businessId: string
  customerId: string
  serviceId: string
  staffId: string | null
  /** Computed on every read from joinedAt ordering — never a mutable client-facing fact. */
  position: number
  status: QueueEntryStatus
  joinedAt: string
  calledAt: string | null
  startedAt: string | null
  completedAt: string | null
  estimatedWaitMinutes: number | null
}

/** businessId comes from the path; only allowed when the business has QUEUE capability. */
export interface CreateQueueEntryRequest {
  serviceId: string
  staffId?: string | null
}

/** Customer-facing "where am I" view — GET /me/queue-entries/{id}/status. */
export interface QueueStatusResponse {
  status: QueueEntryStatus
  peopleAhead: number
  estimatedWaitMinutes: number | null
}

export interface QueueEntryListParams {
  status?: QueueEntryStatus
  [key: string]: string | number | boolean | undefined | null
}

import { screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MyQueuePage } from '../features/queue/MyQueuePage'
import { customerQueueApi } from '../services/api/queueApi'
import { renderWithProviders } from '../test-utils'
import { ApiError } from '../utils/apiError'
import type { QueueEntryDto } from '../types'

vi.mock('../services/api/queueApi')

const waitingEntry: QueueEntryDto = {
  id: 'entry-1',
  businessId: 'biz-1',
  customerId: 'cust-1',
  serviceId: 'svc-1',
  staffId: null,
  position: 2,
  status: 'WAITING',
  joinedAt: '2026-09-04T10:00:00Z',
  calledAt: null,
  startedAt: null,
  completedAt: null,
  estimatedWaitMinutes: 15,
}

describe('MyQueuePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders a distinct error state (not a blank screen) when the queue query fails', async () => {
    vi.mocked(customerQueueApi.listMine).mockRejectedValue(
      new ApiError({
        timestamp: '2026-01-01T00:00:00Z',
        status: 500,
        code: 'INTERNAL_ERROR',
        message: 'boom',
        path: '/api/v1/me/queue-entries',
      }),
    )

    renderWithProviders(<MyQueuePage />)

    expect(await screen.findByRole('alert')).toBeInTheDocument()
    expect(screen.getByText(/something went wrong on our end/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument()
  })

  it('renders a distinct empty state when the customer has no queue entries', async () => {
    vi.mocked(customerQueueApi.listMine).mockResolvedValue([])

    renderWithProviders(<MyQueuePage />)

    expect(await screen.findByText(/not in any queue/i)).toBeInTheDocument()
  })

  it('shows live position/wait status for an active entry and lets the customer leave the queue', async () => {
    vi.mocked(customerQueueApi.listMine).mockResolvedValue([waitingEntry])
    vi.mocked(customerQueueApi.getStatus).mockResolvedValue({
      status: 'WAITING',
      peopleAhead: 1,
      estimatedWaitMinutes: 15,
    })
    vi.mocked(customerQueueApi.cancel).mockResolvedValue({ ...waitingEntry, status: 'CANCELLED' })

    renderWithProviders(<MyQueuePage />)

    expect(await screen.findByText(/1 person ahead of you/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /leave queue/i })).toBeInTheDocument()

    await waitFor(() => expect(customerQueueApi.getStatus).toHaveBeenCalledWith('entry-1'))
  })
})

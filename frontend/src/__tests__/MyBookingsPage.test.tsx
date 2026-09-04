import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MyBookingsPage } from '../features/booking/MyBookingsPage'
import { customerBookingApi } from '../services/api/bookingApi'
import { renderWithProviders } from '../test-utils'
import { ApiError } from '../utils/apiError'

vi.mock('../services/api/bookingApi')

describe('MyBookingsPage — API error state', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders a distinct error state (not a blank screen) when the bookings query fails', async () => {
    vi.mocked(customerBookingApi.listMine).mockRejectedValue(
      new ApiError({
        timestamp: '2026-01-01T00:00:00Z',
        status: 500,
        code: 'INTERNAL_ERROR',
        message: 'boom',
        path: '/api/v1/me/bookings',
      }),
    )

    renderWithProviders(<MyBookingsPage />)

    expect(await screen.findByRole('alert')).toBeInTheDocument()
    expect(screen.getByText(/something went wrong on our end/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument()
  })

  it('retries the query when "Try again" is clicked', async () => {
    const user = userEvent.setup()
    vi.mocked(customerBookingApi.listMine)
      .mockRejectedValueOnce(
        new ApiError({
          timestamp: '2026-01-01T00:00:00Z',
          status: 500,
          code: 'INTERNAL_ERROR',
          message: 'boom',
          path: '/api/v1/me/bookings',
        }),
      )
      .mockResolvedValueOnce({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, hasNext: false })

    renderWithProviders(<MyBookingsPage />)

    await screen.findByRole('alert')
    await user.click(screen.getByRole('button', { name: /try again/i }))

    await waitFor(() => expect(customerBookingApi.listMine).toHaveBeenCalledTimes(2))
    expect(await screen.findByText(/no bookings yet/i)).toBeInTheDocument()
  })

  it('renders a distinct empty state when there are no bookings', async () => {
    vi.mocked(customerBookingApi.listMine).mockResolvedValue({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      hasNext: false,
    })

    renderWithProviders(<MyBookingsPage />)

    expect(await screen.findByText(/no bookings yet/i)).toBeInTheDocument()
  })
})

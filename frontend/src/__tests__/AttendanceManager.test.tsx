import { screen } from '@testing-library/react'
import { Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AttendanceManager } from '../features/dashboard/AttendanceManager'
import { attendanceApi } from '../services/api/attendanceApi'
import { renderWithProviders } from '../test-utils'
import type { AttendanceDto, CapacityResponse } from '../types'

vi.mock('../services/api/attendanceApi')

function renderPage() {
  return renderWithProviders(
    <Routes>
      <Route path="/dashboard/:businessId/attendance" element={<AttendanceManager />} />
    </Routes>,
    { route: '/dashboard/biz-1/attendance' },
  )
}

describe('AttendanceManager', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows a loading state while capacity/attendance are being fetched', () => {
    vi.mocked(attendanceApi.getCapacity).mockReturnValue(new Promise(() => {}))
    vi.mocked(attendanceApi.list).mockReturnValue(new Promise(() => {}))

    renderPage()

    expect(screen.getAllByText(/loading/i).length).toBeGreaterThan(0)
  })

  it('shows an empty state when nobody is checked in', async () => {
    const capacity: CapacityResponse = { current: 0, capacity: 50, available: 50 }
    vi.mocked(attendanceApi.getCapacity).mockResolvedValue(capacity)
    vi.mocked(attendanceApi.list).mockResolvedValue({
      content: [],
      page: 0,
      size: 100,
      totalElements: 0,
      totalPages: 0,
      hasNext: false,
    })

    renderPage()

    expect(await screen.findByText('0')).toBeInTheDocument()
    expect(await screen.findByText(/\/ 50 capacity/)).toBeInTheDocument()
    expect(await screen.findByText(/nobody checked in/i)).toBeInTheDocument()
  })

  it('lists currently checked-in customers', async () => {
    const capacity: CapacityResponse = { current: 1, capacity: 50, available: 49 }
    const record: AttendanceDto = {
      id: 'att-1',
      businessId: 'biz-1',
      customerId: 'cust-12345678',
      checkInAt: '2026-09-04T10:00:00Z',
      checkOutAt: null,
    }
    vi.mocked(attendanceApi.getCapacity).mockResolvedValue(capacity)
    vi.mocked(attendanceApi.list).mockResolvedValue({
      content: [record],
      page: 0,
      size: 100,
      totalElements: 1,
      totalPages: 1,
      hasNext: false,
    })

    renderPage()

    expect(await screen.findByText(/Customer #cust-123/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /check out/i })).toBeInTheDocument()
  })

  it('shows an error state with a retry option when capacity fails to load', async () => {
    vi.mocked(attendanceApi.getCapacity).mockRejectedValue(new Error('NETWORK_ERROR'))
    vi.mocked(attendanceApi.list).mockResolvedValue({
      content: [],
      page: 0,
      size: 100,
      totalElements: 0,
      totalPages: 0,
      hasNext: false,
    })

    renderPage()

    expect(await screen.findByRole('alert')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument()
  })
})

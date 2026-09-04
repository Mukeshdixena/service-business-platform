import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { BookingFlow } from '../features/booking/BookingFlow'
import { availabilityApi } from '../services/api/availabilityApi'
import { customerBookingApi } from '../services/api/bookingApi'
import { discoveryApi } from '../services/api/discoveryApi'
import { renderWithProviders } from '../test-utils'
import type { BusinessPublicDto, AvailabilityResponse, BookingDto } from '../types'

vi.mock('../services/api/discoveryApi')
vi.mock('../services/api/availabilityApi')
vi.mock('../services/api/bookingApi')

const business: BusinessPublicDto = {
  id: 'biz-1',
  name: 'Glow Salon',
  slug: 'glow-salon',
  description: 'A lovely salon',
  phone: null,
  email: null,
  logoUrl: null,
  coverImageUrl: null,
  category: 'SALON',
  capabilities: ['APPOINTMENTS'],
  locations: [],
  services: [
    {
      id: 'svc-1',
      businessId: 'biz-1',
      name: 'Haircut',
      description: null,
      price: 200,
      currency: 'INR',
      durationMinutes: 30,
      bookingType: 'APPOINTMENT',
      pricingUnit: null,
      status: 'ACTIVE',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    },
  ],
  staff: [],
  hours: [],
}

const availability: AvailabilityResponse = {
  status: 'AVAILABLE',
  type: 'APPOINTMENT',
  date: '2026-09-10',
  slots: [
    { start: '2026-09-10T09:00:00Z', end: '2026-09-10T09:30:00Z', available: true, staffId: null },
    { start: '2026-09-10T09:30:00Z', end: '2026-09-10T10:00:00Z', available: false, staffId: null },
  ],
}

const createdBooking: BookingDto = {
  id: 'booking-1',
  businessId: 'biz-1',
  customerId: 'cust-1',
  serviceId: 'svc-1',
  staffId: null,
  resourceId: null,
  startAt: '2026-09-10T09:00:00Z',
  endAt: '2026-09-10T09:30:00Z',
  status: 'PENDING',
  price: 200,
  currency: 'INR',
  notes: null,
  createdAt: '2026-09-04T00:00:00Z',
  updatedAt: '2026-09-04T00:00:00Z',
}

function renderBookingFlow() {
  return renderWithProviders(
    <Routes>
      <Route path="/book/:slug" element={<BookingFlow />} />
    </Routes>,
    { route: '/book/glow-salon?serviceId=svc-1' },
  )
}

describe('BookingFlow — slot selection and confirmation', () => {
  beforeEach(() => {
    vi.mocked(discoveryApi.getBusinessBySlug).mockResolvedValue(business)
    vi.mocked(availabilityApi.get).mockResolvedValue(availability)
    vi.mocked(customerBookingApi.create).mockResolvedValue(createdBooking)
  })

  it('lets the customer pick an available slot and confirm the booking', async () => {
    const user = userEvent.setup()
    renderBookingFlow()

    // Service is preselected from the ?serviceId= query param.
    expect(await screen.findByText('Haircut')).toBeInTheDocument()

    // Wait for availability to load and render slot options.
    const slots = await screen.findAllByRole('option')
    expect(slots).toHaveLength(2)

    const availableSlot = slots[0]
    const unavailableSlot = slots[1]
    expect(availableSlot).toBeEnabled()
    expect(unavailableSlot).toBeDisabled()

    // The confirm step only appears once a slot is chosen.
    expect(screen.queryByRole('button', { name: /confirm booking/i })).not.toBeInTheDocument()

    await user.click(availableSlot)

    const confirmButton = await screen.findByRole('button', { name: /confirm booking/i })
    await user.click(confirmButton)

    await waitFor(() =>
      expect(customerBookingApi.create).toHaveBeenCalledWith('biz-1', {
        serviceId: 'svc-1',
        staffId: null,
        startAt: '2026-09-10T09:00:00Z',
        notes: null,
      }),
    )

    expect(await screen.findByText(/booking requested/i)).toBeInTheDocument()
  })

  it('does not allow selecting an unavailable slot', async () => {
    renderBookingFlow()

    const slots = await screen.findAllByRole('option')
    const unavailableSlot = slots[1]

    // Disabled slots can't be clicked at all (jsdom no-ops clicks on disabled buttons),
    // so the confirm step never becomes reachable through this slot.
    expect(unavailableSlot).toBeDisabled()
    expect(screen.queryByRole('button', { name: /confirm booking/i })).not.toBeInTheDocument()
    expect(customerBookingApi.create).not.toHaveBeenCalled()
  })
})

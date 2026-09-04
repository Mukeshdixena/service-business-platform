import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { BusinessProfilePage } from '../features/discovery/BusinessProfilePage'
import { discoveryApi } from '../services/api/discoveryApi'
import { customerClassApi } from '../services/api/classApi'
import { userApi } from '../services/api/userApi'
import { authApi } from '../services/api/authApi'
import { renderWithProviders } from '../test-utils'
import type { AuthResponse, BusinessPublicDto, ClassDto, ClassEnrollmentDto } from '../types'

vi.mock('../services/api/discoveryApi')
vi.mock('../services/api/classApi')
vi.mock('../services/api/userApi')
vi.mock('../services/api/authApi')

const openClass: ClassDto = {
  id: 'class-1',
  businessId: 'biz-1',
  name: 'Morning Yoga',
  description: 'A gentle start to the day',
  staffId: null,
  startAt: '2026-09-10T09:00:00Z',
  endAt: '2026-09-10T10:00:00Z',
  capacity: 20,
  enrolledCount: 5,
  status: 'SCHEDULED',
}

const fullClass: ClassDto = {
  ...openClass,
  id: 'class-2',
  name: 'Evening HIIT',
  capacity: 10,
  enrolledCount: 10,
}

function business(classes: ClassDto[]): BusinessPublicDto {
  return {
    id: 'biz-1',
    name: 'Iron Gym',
    slug: 'iron-gym',
    description: 'A gym',
    phone: null,
    email: null,
    logoUrl: null,
    coverImageUrl: null,
    category: 'GYM',
    capabilities: ['CLASSES'],
    locations: [],
    services: [],
    staff: [],
    hours: [],
    classes,
  }
}

const auth: AuthResponse = {
  accessToken: 'token',
  tokenType: 'Bearer',
  expiresIn: 900,
  user: { id: 'cust-1', email: 'a@b.com', fullName: 'Jane Doe', roles: ['CUSTOMER'], createdAt: '2026-01-01T00:00:00Z' },
}

function renderProfilePage() {
  return renderWithProviders(
    <Routes>
      <Route path="/businesses/:slug" element={<BusinessProfilePage />} />
    </Routes>,
    { route: '/businesses/iron-gym', withAuth: true },
  )
}

describe('Class enrollment flow (smoke test)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(userApi.getMyBusinesses).mockResolvedValue([])
    vi.mocked(authApi.refresh).mockResolvedValue(auth)
  })

  it('lets a signed-in customer enroll in an open class', async () => {
    vi.mocked(discoveryApi.getBusinessBySlug).mockResolvedValue(business([openClass]))
    const enrolled: ClassEnrollmentDto = {
      id: 'enr-1',
      classId: 'class-1',
      customerId: 'cust-1',
      status: 'ENROLLED',
      createdAt: '2026-09-04T00:00:00Z',
    }
    vi.mocked(customerClassApi.enroll).mockResolvedValue(enrolled)

    const user = userEvent.setup()
    renderProfilePage()

    expect(await screen.findByText('Morning Yoga')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /enroll/i }))

    await waitFor(() => expect(customerClassApi.enroll).toHaveBeenCalledWith('biz-1', 'class-1'))
  })

  it('offers "Join waitlist" and enrolls as WAITLISTED when the class is full', async () => {
    vi.mocked(discoveryApi.getBusinessBySlug).mockResolvedValue(business([fullClass]))
    const waitlisted: ClassEnrollmentDto = {
      id: 'enr-2',
      classId: 'class-2',
      customerId: 'cust-1',
      status: 'WAITLISTED',
      createdAt: '2026-09-04T00:00:00Z',
    }
    vi.mocked(customerClassApi.enroll).mockResolvedValue(waitlisted)

    const user = userEvent.setup()
    renderProfilePage()

    expect(await screen.findByText('Evening HIIT')).toBeInTheDocument()
    expect(screen.getByText(/10\/10 enrolled/)).toBeInTheDocument()

    const waitlistButton = screen.getByRole('button', { name: /join waitlist/i })
    await user.click(waitlistButton)

    await waitFor(() => expect(customerClassApi.enroll).toHaveBeenCalledWith('biz-1', 'class-2'))
  })
})

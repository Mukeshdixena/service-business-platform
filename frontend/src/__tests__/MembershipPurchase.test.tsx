import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { BusinessProfilePage } from '../features/discovery/BusinessProfilePage'
import { discoveryApi } from '../services/api/discoveryApi'
import { customerMembershipApi } from '../services/api/membershipApi'
import { userApi } from '../services/api/userApi'
import { authApi } from '../services/api/authApi'
import { renderWithProviders } from '../test-utils'
import type { BusinessPublicDto, MembershipDto, MembershipPlanDto, AuthResponse } from '../types'

vi.mock('../services/api/discoveryApi')
vi.mock('../services/api/membershipApi')
vi.mock('../services/api/userApi')
vi.mock('../services/api/authApi')

const plan: MembershipPlanDto = {
  id: 'plan-1',
  businessId: 'biz-1',
  name: 'Monthly',
  description: 'Unlimited monthly access',
  price: 1500,
  currency: 'INR',
  duration: 1,
  durationUnit: 'MONTH',
  status: 'ACTIVE',
}

// Membership plans are browsed via the public discovery response's own
// `membershipPlans` field (API_CONTRACT.md), not the OWNER/STAFF-only
// GET /businesses/{businessId}/membership-plans management endpoint.
const business: BusinessPublicDto = {
  id: 'biz-1',
  name: 'Iron Gym',
  slug: 'iron-gym',
  description: 'A gym',
  phone: null,
  email: null,
  logoUrl: null,
  coverImageUrl: null,
  category: 'GYM',
  capabilities: ['MEMBERSHIPS'],
  locations: [],
  services: [],
  staff: [],
  hours: [],
  membershipPlans: [plan],
}

const purchasedMembership: MembershipDto = {
  id: 'membership-1',
  businessId: 'biz-1',
  customerId: 'cust-1',
  membershipPlanId: 'plan-1',
  startDate: '2026-09-04',
  endDate: '2026-10-04',
  status: 'ACTIVE',
  paymentId: null,
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

describe('Membership purchase flow (smoke test)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(discoveryApi.getBusinessBySlug).mockResolvedValue(business)
    vi.mocked(customerMembershipApi.purchase).mockResolvedValue(purchasedMembership)
    vi.mocked(userApi.getMyBusinesses).mockResolvedValue([])
    vi.mocked(authApi.refresh).mockResolvedValue(auth)
  })

  it('lets a signed-in customer buy a membership plan from the business profile page', async () => {
    const user = userEvent.setup()
    renderProfilePage()

    expect(await screen.findByText('Monthly')).toBeInTheDocument()

    const buyButton = screen.getByRole('button', { name: /buy/i })
    await user.click(buyButton)

    await waitFor(() =>
      expect(customerMembershipApi.purchase).toHaveBeenCalledWith('biz-1', { membershipPlanId: 'plan-1' }),
    )
  })
})

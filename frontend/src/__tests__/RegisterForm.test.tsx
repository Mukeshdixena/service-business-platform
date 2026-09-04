import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { RegisterForm } from '../features/auth/RegisterForm'
import { authApi } from '../services/api/authApi'
import { userApi } from '../services/api/userApi'
import { renderWithProviders } from '../test-utils'
import { ApiError } from '../utils/apiError'

vi.mock('../services/api/authApi')
vi.mock('../services/api/userApi')

describe('RegisterForm', () => {
  beforeEach(() => {
    vi.mocked(authApi.refresh).mockRejectedValue(new Error('NETWORK_ERROR'))
    vi.mocked(userApi.getMyBusinesses).mockResolvedValue([])
  })

  it('validates required fields and a minimum password length', async () => {
    const user = userEvent.setup()
    renderWithProviders(<RegisterForm />, { withAuth: true })

    await user.type(screen.getByLabelText(/full name/i), 'Jane Doe')
    await user.type(screen.getByLabelText(/email/i), 'jane@example.com')
    await user.type(screen.getByLabelText(/password/i), 'short')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    expect(await screen.findByText(/at least 8 characters/i)).toBeInTheDocument()
    expect(authApi.register).not.toHaveBeenCalled()
  })

  it('submits the chosen role along with the rest of the form', async () => {
    const user = userEvent.setup()
    vi.mocked(authApi.register).mockResolvedValue({
      accessToken: 'token',
      tokenType: 'Bearer',
      expiresIn: 900,
      user: {
        id: 'u1',
        email: 'owner@example.com',
        fullName: 'Owner Person',
        roles: ['BUSINESS_OWNER'],
        createdAt: '2026-01-01T00:00:00Z',
      },
    })

    renderWithProviders(<RegisterForm />, { withAuth: true })

    await user.click(screen.getByText(/i own a business/i))
    await user.type(screen.getByLabelText(/full name/i), 'Owner Person')
    await user.type(screen.getByLabelText(/email/i), 'owner@example.com')
    await user.type(screen.getByLabelText(/password/i), 'password123')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    await waitFor(() =>
      expect(authApi.register).toHaveBeenCalledWith({
        fullName: 'Owner Person',
        email: 'owner@example.com',
        password: 'password123',
        role: 'BUSINESS_OWNER',
      }),
    )
  })

  it('surfaces a backend validation error (e.g. duplicate email)', async () => {
    const user = userEvent.setup()
    vi.mocked(authApi.register).mockRejectedValue(
      new ApiError({
        timestamp: '2026-01-01T00:00:00Z',
        status: 400,
        code: 'VALIDATION_ERROR',
        message: 'Email is already registered.',
        path: '/api/v1/auth/register',
      }),
    )

    renderWithProviders(<RegisterForm />, { withAuth: true })

    await user.type(screen.getByLabelText(/full name/i), 'Jane Doe')
    await user.type(screen.getByLabelText(/email/i), 'jane@example.com')
    await user.type(screen.getByLabelText(/password/i), 'password123')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    expect(await screen.findByText(/email is already registered/i)).toBeInTheDocument()
  })
})

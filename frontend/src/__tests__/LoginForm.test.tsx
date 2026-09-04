import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { LoginForm } from '../features/auth/LoginForm'
import { authApi } from '../services/api/authApi'
import { userApi } from '../services/api/userApi'
import { renderWithProviders } from '../test-utils'
import { ApiError } from '../utils/apiError'

vi.mock('../services/api/authApi')
vi.mock('../services/api/userApi')

describe('LoginForm', () => {
  beforeEach(() => {
    vi.mocked(authApi.refresh).mockRejectedValue(new Error('NETWORK_ERROR'))
    vi.mocked(userApi.getMyBusinesses).mockResolvedValue([])
  })

  it('shows field validation errors instead of calling the API when the form is empty', async () => {
    const user = userEvent.setup()
    renderWithProviders(<LoginForm />, { withAuth: true })

    await user.click(screen.getByRole('button', { name: /sign in/i }))

    expect(await screen.findByText(/email is required/i)).toBeInTheDocument()
    expect(screen.getByText(/password is required/i)).toBeInTheDocument()
    expect(authApi.login).not.toHaveBeenCalled()
  })

  it('rejects an invalid email address', async () => {
    const user = userEvent.setup()
    renderWithProviders(<LoginForm />, { withAuth: true })

    await user.type(screen.getByLabelText(/email/i), 'not-an-email')
    await user.type(screen.getByLabelText(/password/i), 'password123')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    expect(await screen.findByText(/enter a valid email/i)).toBeInTheDocument()
    expect(authApi.login).not.toHaveBeenCalled()
  })

  it('submits valid credentials to the API', async () => {
    const user = userEvent.setup()
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: 'token',
      tokenType: 'Bearer',
      expiresIn: 900,
      user: { id: 'u1', email: 'jane@example.com', fullName: 'Jane Doe', roles: ['CUSTOMER'], createdAt: '2026-01-01T00:00:00Z' },
    })

    renderWithProviders(<LoginForm />, { withAuth: true })

    await user.type(screen.getByLabelText(/email/i), 'jane@example.com')
    await user.type(screen.getByLabelText(/password/i), 'password123')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    await waitFor(() =>
      expect(authApi.login).toHaveBeenCalledWith({ email: 'jane@example.com', password: 'password123' }),
    )
  })

  it('surfaces a backend error without a generic fallback message', async () => {
    const user = userEvent.setup()
    vi.mocked(authApi.login).mockRejectedValue(
      new ApiError({
        timestamp: '2026-01-01T00:00:00Z',
        status: 401,
        code: 'UNAUTHENTICATED',
        message: 'Bad credentials',
        path: '/api/v1/auth/login',
      }),
    )

    renderWithProviders(<LoginForm />, { withAuth: true })

    await user.type(screen.getByLabelText(/email/i), 'jane@example.com')
    await user.type(screen.getByLabelText(/password/i), 'wrong-password')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    expect(await screen.findByText(/incorrect email or password/i)).toBeInTheDocument()
  })
})

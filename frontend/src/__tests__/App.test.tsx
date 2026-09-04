import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { App } from '../app/App'
import { authApi } from '../services/api/authApi'
import { discoveryApi } from '../services/api/discoveryApi'
import { userApi } from '../services/api/userApi'

vi.mock('../services/api/authApi')
vi.mock('../services/api/userApi')
vi.mock('../services/api/discoveryApi')

describe('App', () => {
  beforeEach(() => {
    vi.mocked(authApi.refresh).mockRejectedValue(new Error('NETWORK_ERROR'))
    vi.mocked(userApi.getMyBusinesses).mockResolvedValue([])
    vi.mocked(discoveryApi.listCategories).mockResolvedValue([{ value: 'SALON', label: 'Salon' }])
    vi.mocked(discoveryApi.searchBusinesses).mockResolvedValue({
      content: [],
      page: 0,
      size: 12,
      totalElements: 0,
      totalPages: 0,
      hasNext: false,
    })
  })

  it('wires up providers/router and renders the landing (discovery) page at "/"', async () => {
    render(<App />)

    // Navbar (logged-out state) and the discovery page heading should both render.
    expect(await screen.findByRole('heading', { name: /find a business/i })).toBeInTheDocument()
    expect(screen.getByText('Service Business Platform')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /log in/i })).toBeInTheDocument()
    expect(await screen.findByText(/no businesses found/i)).toBeInTheDocument()
  })
})

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactElement, ReactNode } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { AuthProvider } from './features/auth/AuthContext'
import { ToastProvider } from './hooks/useToast'

/** A fresh, no-retry QueryClient per test — avoids react-query's default retry/backoff slowing down error-path tests. */
export function createTestQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })
}

interface RenderOptions {
  route?: string
  queryClient?: QueryClient
  /** Wrap with AuthProvider — only needed for components that call useAuth(). Off by default to avoid unrelated network calls in tests. */
  withAuth?: boolean
}

function Providers({
  children,
  queryClient,
  route,
  withAuth,
}: {
  children: ReactNode
  queryClient: QueryClient
  route: string
  withAuth: boolean
}) {
  const content = (
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <MemoryRouter initialEntries={[route]}>{children}</MemoryRouter>
      </ToastProvider>
    </QueryClientProvider>
  )

  if (!withAuth) return content

  return (
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <AuthProvider>
          <MemoryRouter initialEntries={[route]}>{children}</MemoryRouter>
        </AuthProvider>
      </ToastProvider>
    </QueryClientProvider>
  )
}

export function renderWithProviders(ui: ReactElement, options: RenderOptions = {}) {
  const queryClient = options.queryClient ?? createTestQueryClient()
  const route = options.route ?? '/'
  const withAuth = options.withAuth ?? false

  return {
    queryClient,
    ...render(
      <Providers queryClient={queryClient} route={route} withAuth={withAuth}>
        {ui}
      </Providers>,
    ),
  }
}

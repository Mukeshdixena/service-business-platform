import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { LoadingSpinner } from '../components/ui'
import { useAuth } from '../features/auth/AuthContext'

/**
 * UX-only gate (CLAUDE_CODE.md §30) — the backend is the real authorization
 * boundary. This just avoids flashing authenticated-only screens to a
 * logged-out visitor and sends them to /login with a return path.
 */
export function RequireAuth() {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'loading') {
    return <LoadingSpinner label="Checking your session…" className="min-h-[50vh]" />
  }
  if (status === 'unauthenticated') {
    return <Navigate to="/login" replace state={{ from: `${location.pathname}${location.search}` }} />
  }
  return <Outlet />
}

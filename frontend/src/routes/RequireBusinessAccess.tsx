import { Navigate, Outlet, useParams } from 'react-router-dom'
import { LoadingSpinner } from '../components/ui'
import { useAuth } from '../features/auth/AuthContext'

/**
 * UX-only gate for /dashboard/:businessId/** — confirms the signed-in user
 * has a BusinessMembership on this businessId before showing dashboard
 * screens for it. The backend re-checks membership on every request
 * regardless (CLAUDE_CODE.md §31) — never trust businessId from the URL.
 */
export function RequireBusinessAccess() {
  const { businessId } = useParams<{ businessId: string }>()
  const { status, memberships, membershipsLoaded } = useAuth()

  if (status === 'loading' || !membershipsLoaded) {
    return <LoadingSpinner label="Loading your businesses…" className="min-h-[50vh]" />
  }
  if (status === 'unauthenticated') {
    return <Navigate to="/login" replace />
  }
  const hasAccess = memberships.some((membership) => membership.businessId === businessId)
  if (!hasAccess) {
    return <Navigate to="/dashboard" replace />
  }
  return <Outlet />
}

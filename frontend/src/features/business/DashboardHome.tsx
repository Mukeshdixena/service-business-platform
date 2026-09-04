import { Navigate } from 'react-router-dom'
import { Card, LoadingSpinner } from '../../components/ui'
import { useAuth } from '../auth/AuthContext'
import { CreateBusinessForm } from './CreateBusinessForm'

/**
 * Landing point for /dashboard: sends an owner straight to their (first)
 * business, or prompts them to create one if they have none yet. Business
 * creation isn't explicitly enumerated in this pass's scope, but the rest of
 * the dashboard (profile/services/staff/hours/bookings) is unreachable
 * without it, so it's included as necessary onboarding glue.
 */
export function DashboardHome() {
  const { memberships, membershipsLoaded } = useAuth()

  if (!membershipsLoaded) {
    return <LoadingSpinner label="Loading your businesses…" className="min-h-[50vh]" />
  }

  if (memberships.length > 0) {
    return <Navigate to={`/dashboard/${memberships[0]?.businessId}/profile`} replace />
  }

  return (
    <div className="mx-auto max-w-lg">
      <Card title="Create your business">
        <p className="mb-4 text-sm text-slate-500">
          Set up your business profile to start adding services, staff, and hours.
        </p>
        <CreateBusinessForm />
      </Card>
    </div>
  )
}

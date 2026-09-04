import { useQuery } from '@tanstack/react-query'
import { NavLink, Outlet, useNavigate, useParams } from 'react-router-dom'
import { Select } from '../../components/ui'
import { useAuth } from '../auth/AuthContext'
import { queryKeys } from '../../hooks/queryKeys'
import { businessApi } from '../../services/api/businessApi'
import { cn } from '../../utils/cn'

const BASE_TABS = [
  { to: 'profile', label: 'Profile' },
  { to: 'services', label: 'Services' },
  { to: 'staff', label: 'Staff' },
  { to: 'hours', label: 'Hours' },
  { to: 'bookings', label: 'Bookings' },
]

export function DashboardLayout() {
  const { businessId } = useParams<{ businessId: string }>()
  const { memberships } = useAuth()
  const navigate = useNavigate()

  // Queue/Memberships tabs are only shown for businesses with those capabilities enabled.
  const businessQuery = useQuery({
    queryKey: queryKeys.business(businessId ?? ''),
    queryFn: () => businessApi.get(businessId ?? ''),
    enabled: Boolean(businessId),
  })
  const capabilities = businessQuery.data?.capabilities ?? []
  const tabs = [
    ...BASE_TABS,
    ...(capabilities.includes('QUEUE') ? [{ to: 'queue', label: 'Queue' }] : []),
    ...(capabilities.includes('MEMBERSHIPS')
      ? [
          { to: 'membership-plans', label: 'Membership plans' },
          { to: 'memberships', label: 'Memberships' },
        ]
      : []),
    ...(capabilities.includes('CAPACITY') ? [{ to: 'attendance', label: 'Attendance' }] : []),
    ...(capabilities.includes('CLASSES') ? [{ to: 'classes', label: 'Classes' }] : []),
    ...(capabilities.includes('RESOURCES') || capabilities.includes('RENTALS')
      ? [{ to: 'resources', label: 'Resources' }]
      : []),
  ]

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-2xl font-semibold text-slate-900">Business dashboard</h1>
        {memberships.length > 1 && (
          <div className="w-64">
            <Select
              options={memberships.map((membership) => ({ value: membership.businessId, label: membership.businessName }))}
              value={businessId}
              onChange={(event) => navigate(`/dashboard/${event.target.value}/profile`)}
            />
          </div>
        )}
      </div>

      <div className="flex gap-1 overflow-x-auto border-b border-slate-200">
        {tabs.map((tab) => (
          <NavLink
            key={tab.to}
            to={tab.to}
            className={({ isActive }) =>
              cn(
                'shrink-0 border-b-2 px-3 py-2 text-sm font-medium',
                isActive ? 'border-brand-600 text-brand-700' : 'border-transparent text-slate-500 hover:text-slate-700',
              )
            }
          >
            {tab.label}
          </NavLink>
        ))}
      </div>

      <Outlet />
    </div>
  )
}

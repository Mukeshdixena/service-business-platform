import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Badge, Button, EmptyState, ErrorState, LoadingSpinner, Pagination, Select } from '../../components/ui'
import { adminApi } from '../../services/api/adminApi'
import { useToast } from '../../hooks/useToast'
import { formatDateTime } from '../../utils/date'

const PAGE_SIZE = 20

const STATUS_OPTIONS = [
  { value: '', label: 'All statuses' },
  { value: 'DRAFT', label: 'Draft' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'SUSPENDED', label: 'Suspended' },
  { value: 'ARCHIVED', label: 'Archived' },
]

function VerificationBadge({ status }: { status: string }) {
  const colorMap: Record<string, 'green' | 'amber' | 'red' | 'slate'> = {
    UNVERIFIED: 'slate',
    PENDING: 'amber',
    VERIFIED: 'green',
    REJECTED: 'red',
  }
  return <Badge color={colorMap[status] ?? 'slate'}>{status.replace(/_/g, ' ')}</Badge>
}

function StatusBadge({ status }: { status: string }) {
  const colorMap: Record<string, 'green' | 'amber' | 'red' | 'slate'> = {
    DRAFT: 'slate',
    ACTIVE: 'green',
    SUSPENDED: 'red',
    ARCHIVED: 'slate',
  }
  return <Badge color={colorMap[status] ?? 'slate'}>{status}</Badge>
}

export function AdminDashboard() {
  const [statusFilter, setStatusFilter] = useState('')
  const [page, setPage] = useState(0)
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const statsQuery = useQuery({
    queryKey: ['admin', 'stats'],
    queryFn: () => adminApi.getStats(),
  })

  const businessesQuery = useQuery({
    queryKey: ['admin', 'businesses', { status: statusFilter, page }],
    queryFn: () => adminApi.listBusinesses({ status: statusFilter || undefined, page, size: PAGE_SIZE }),
  })

  const verifyMutation = useMutation({
    mutationFn: (vars: { id: string; status: string }) => adminApi.verifyBusiness(vars.id, vars.status),
    onSuccess: () => {
      showToast('Business verification updated.', 'success')
      void queryClient.invalidateQueries({ queryKey: ['admin'] })
    },
  })

  const suspendMutation = useMutation({
    mutationFn: (id: string) => adminApi.suspendBusiness(id),
    onSuccess: () => {
      showToast('Business suspended.', 'success')
      void queryClient.invalidateQueries({ queryKey: ['admin'] })
    },
  })

  const activateMutation = useMutation({
    mutationFn: (id: string) => adminApi.activateBusiness(id),
    onSuccess: () => {
      showToast('Business activated.', 'success')
      void queryClient.invalidateQueries({ queryKey: ['admin'] })
    },
  })

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-semibold text-slate-900">Admin Dashboard</h1>

      {statsQuery.isLoading && <LoadingSpinner label="Loading stats…" />}
      {statsQuery.isSuccess && (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <p className="text-sm text-slate-500">Total businesses</p>
            <p className="mt-1 text-2xl font-semibold text-slate-900">{statsQuery.data.totalBusinesses}</p>
          </div>
          <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <p className="text-sm text-slate-500">Active businesses</p>
            <p className="mt-1 text-2xl font-semibold text-slate-900">{statsQuery.data.activeBusinesses}</p>
          </div>
          <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <p className="text-sm text-slate-500">Total users</p>
            <p className="mt-1 text-2xl font-semibold text-slate-900">{statsQuery.data.totalUsers}</p>
          </div>
          <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <p className="text-sm text-slate-500">Pending reviews</p>
            <p className="mt-1 text-2xl font-semibold text-slate-900">{statsQuery.data.pendingReviews}</p>
          </div>
        </div>
      )}

      <div className="flex flex-wrap items-center justify-between gap-4">
        <h2 className="text-lg font-semibold text-slate-900">Businesses</h2>
        <div className="w-48">
          <Select
            options={STATUS_OPTIONS}
            value={statusFilter}
            onChange={(event) => {
              setStatusFilter(event.target.value)
              setPage(0)
            }}
          />
        </div>
      </div>

      {businessesQuery.isLoading && <LoadingSpinner label="Loading businesses…" />}
      {businessesQuery.isError && <ErrorState error={businessesQuery.error} onRetry={() => businessesQuery.refetch()} />}
      {businessesQuery.isSuccess && businessesQuery.data.content.length === 0 && (
        <EmptyState title="No businesses" description="No businesses match the current filter." />
      )}

      {businessesQuery.isSuccess && businessesQuery.data.content.length > 0 && (
        <>
          <div className="flex flex-col gap-3">
            {businessesQuery.data.content.map((business) => (
              <div
                key={business.id}
                className="flex flex-col gap-3 rounded-lg border border-slate-200 bg-white p-4 shadow-sm sm:flex-row sm:items-center sm:justify-between"
              >
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="font-medium text-slate-900">{business.name}</h3>
                    <StatusBadge status={business.status} />
                    <VerificationBadge status={business.verificationStatus} />
                  </div>
                  <p className="mt-1 text-sm text-slate-500">
                    {business.category.replace(/_/g, ' ')} &middot; {formatDateTime(business.createdAt)}
                  </p>
                </div>
                <div className="flex shrink-0 flex-wrap gap-2">
                  {business.verificationStatus !== 'VERIFIED' && (
                    <Button
                      size="sm"
                      variant="primary"
                      isLoading={verifyMutation.isPending && verifyMutation.variables?.id === business.id}
                      onClick={() => verifyMutation.mutate({ id: business.id, status: 'VERIFIED' })}
                    >
                      Verify
                    </Button>
                  )}
                  {business.verificationStatus !== 'REJECTED' && business.verificationStatus !== 'VERIFIED' && (
                    <Button
                      size="sm"
                      variant="danger"
                      isLoading={verifyMutation.isPending && verifyMutation.variables?.id === business.id}
                      onClick={() => verifyMutation.mutate({ id: business.id, status: 'REJECTED' })}
                    >
                      Reject
                    </Button>
                  )}
                  {business.status !== 'SUSPENDED' && (
                    <Button
                      size="sm"
                      variant="danger"
                      isLoading={suspendMutation.isPending && suspendMutation.variables === business.id}
                      onClick={() => suspendMutation.mutate(business.id)}
                    >
                      Suspend
                    </Button>
                  )}
                  {business.status === 'SUSPENDED' && (
                    <Button
                      size="sm"
                      variant="primary"
                      isLoading={activateMutation.isPending && activateMutation.variables === business.id}
                      onClick={() => activateMutation.mutate(business.id)}
                    >
                      Activate
                    </Button>
                  )}
                </div>
              </div>
            ))}
          </div>
          <Pagination
            page={businessesQuery.data.page}
            totalPages={businessesQuery.data.totalPages}
            onPageChange={setPage}
          />
        </>
      )}
    </div>
  )
}

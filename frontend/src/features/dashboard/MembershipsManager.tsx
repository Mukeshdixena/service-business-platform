import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { MembershipCard } from '../../components/membership/MembershipCard'
import { Button, EmptyState, ErrorState, LoadingSpinner, Pagination, Select } from '../../components/ui'
import type { ButtonVariant } from '../../components/ui'
import { queryKeys } from '../../hooks/queryKeys'
import { useToast } from '../../hooks/useToast'
import { businessMembershipApi, membershipPlanApi } from '../../services/api/membershipApi'
import { MEMBERSHIP_STATUSES } from '../../types'
import type { MembershipDto, MembershipStatus } from '../../types'
import { getFriendlyErrorMessage } from '../../utils/apiError'
import type { MembershipAction } from '../../utils/membershipTransitions'
import { getAvailableMembershipActions } from '../../utils/membershipTransitions'

const PAGE_SIZE = 20

const TONE_TO_VARIANT: Record<'primary' | 'danger' | 'neutral', ButtonVariant> = {
  primary: 'primary',
  danger: 'danger',
  neutral: 'secondary',
}

const STATUS_OPTIONS = [
  { value: '', label: 'All statuses' },
  ...MEMBERSHIP_STATUSES.map((status) => ({ value: status, label: status.charAt(0) + status.slice(1).toLowerCase() })),
]

function runAction(businessId: string, membership: MembershipDto, action: MembershipAction) {
  switch (action) {
    case 'freeze':
      return businessMembershipApi.freeze(businessId, membership.id)
    case 'reactivate':
      return businessMembershipApi.reactivate(businessId, membership.id)
    case 'cancel':
      return businessMembershipApi.cancel(businessId, membership.id)
  }
}

export function MembershipsManager() {
  const { businessId } = useParams<{ businessId: string }>()
  const [statusFilter, setStatusFilter] = useState<MembershipStatus | ''>('')
  const [page, setPage] = useState(0)
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const params = { status: statusFilter || undefined, page, size: PAGE_SIZE }

  const membershipsQuery = useQuery({
    queryKey: queryKeys.businessMemberships(businessId ?? '', params),
    queryFn: () => businessMembershipApi.list(businessId ?? '', params),
    enabled: Boolean(businessId),
  })

  const plansQuery = useQuery({
    queryKey: queryKeys.membershipPlans(businessId ?? '', 0),
    queryFn: () => membershipPlanApi.list(businessId ?? '', { page: 0, size: 100 }),
    enabled: Boolean(businessId),
  })
  const planNameById = new Map((plansQuery.data?.content ?? []).map((plan) => [plan.id, plan.name]))

  const invalidateMemberships = () =>
    queryClient.invalidateQueries({ queryKey: ['businesses', businessId, 'memberships'] })

  const actionMutation = useMutation({
    mutationFn: (vars: { membership: MembershipDto; action: MembershipAction }) =>
      runAction(businessId ?? '', vars.membership, vars.action),
    onSuccess: () => {
      showToast('Membership updated.', 'success')
      void invalidateMemberships()
    },
    onError: (error: unknown) => showToast(getFriendlyErrorMessage(error), 'error'),
  })

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h2 className="text-lg font-semibold text-slate-900">Memberships</h2>
        <div className="w-48">
          <Select
            options={STATUS_OPTIONS}
            value={statusFilter}
            onChange={(event) => {
              setStatusFilter(event.target.value as MembershipStatus | '')
              setPage(0)
            }}
          />
        </div>
      </div>

      {membershipsQuery.isLoading && <LoadingSpinner label="Loading memberships…" />}
      {membershipsQuery.isError && (
        <ErrorState error={membershipsQuery.error} onRetry={() => membershipsQuery.refetch()} />
      )}
      {membershipsQuery.isSuccess && membershipsQuery.data.content.length === 0 && (
        <EmptyState title="No memberships" description="Memberships purchased by customers will appear here." />
      )}

      {membershipsQuery.isSuccess && membershipsQuery.data.content.length > 0 && (
        <>
          <div className="flex flex-col gap-3">
            {membershipsQuery.data.content.map((membership) => (
              <MembershipCard
                key={membership.id}
                membership={membership}
                planLabel={planNameById.get(membership.membershipPlanId)}
                actions={getAvailableMembershipActions(membership.status).map((config) => (
                  <Button
                    key={config.action}
                    size="sm"
                    variant={TONE_TO_VARIANT[config.tone]}
                    isLoading={
                      actionMutation.isPending &&
                      actionMutation.variables?.membership.id === membership.id &&
                      actionMutation.variables.action === config.action
                    }
                    onClick={() => actionMutation.mutate({ membership, action: config.action })}
                  >
                    {config.label}
                  </Button>
                ))}
              />
            ))}
          </div>
          <Pagination
            page={membershipsQuery.data.page}
            totalPages={membershipsQuery.data.totalPages}
            onPageChange={setPage}
          />
        </>
      )}
    </div>
  )
}

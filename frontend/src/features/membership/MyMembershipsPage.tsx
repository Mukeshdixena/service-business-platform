import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { MembershipCard } from '../../components/membership/MembershipCard'
import { Button, EmptyState, ErrorState, LoadingSpinner } from '../../components/ui'
import { useEntityLookup } from '../../hooks/useEntityLookup'
import { queryKeys } from '../../hooks/queryKeys'
import { useToast } from '../../hooks/useToast'
import { customerMembershipApi } from '../../services/api/membershipApi'
import type { MembershipDto } from '../../types'
import { customerCanCancelMembership } from '../../utils/membershipTransitions'

export function MyMembershipsPage() {
  const lookup = useEntityLookup()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const membershipsQuery = useQuery({
    queryKey: queryKeys.myMemberships,
    queryFn: () => customerMembershipApi.listMine(),
  })

  const cancelMutation = useMutation({
    mutationFn: (membership: MembershipDto) => customerMembershipApi.cancel(membership.businessId, membership.id),
    onSuccess: () => {
      showToast('Membership cancelled.', 'success')
      void queryClient.invalidateQueries({ queryKey: queryKeys.myMemberships })
    },
  })

  const memberships = membershipsQuery.data ?? []

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">My memberships</h1>
        <p className="mt-1 text-sm text-slate-500">Manage memberships you've purchased across businesses.</p>
      </div>

      {membershipsQuery.isLoading && <LoadingSpinner label="Loading your memberships…" />}
      {membershipsQuery.isError && <ErrorState error={membershipsQuery.error} onRetry={() => membershipsQuery.refetch()} />}

      {membershipsQuery.isSuccess && memberships.length === 0 && (
        <EmptyState title="No memberships yet" description="Memberships you purchase will show up here." />
      )}

      {membershipsQuery.isSuccess && memberships.length > 0 && (
        <div className="flex flex-col gap-3">
          {memberships.map((membership) => {
            const labels = lookup(membership.businessId)
            return (
              <MembershipCard
                key={membership.id}
                membership={membership}
                businessLabel={labels.businessName}
                actions={
                  customerCanCancelMembership(membership.status) ? (
                    <Button
                      variant="danger"
                      size="sm"
                      isLoading={cancelMutation.isPending && cancelMutation.variables?.id === membership.id}
                      onClick={() => cancelMutation.mutate(membership)}
                    >
                      Cancel
                    </Button>
                  ) : undefined
                }
              />
            )
          })}
        </div>
      )}
    </div>
  )
}

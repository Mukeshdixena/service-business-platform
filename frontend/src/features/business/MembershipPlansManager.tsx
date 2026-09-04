import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { MembershipPlanCard } from '../../components/membership/MembershipPlanCard'
import { Button, EmptyState, ErrorState, LoadingSpinner, Modal, Pagination } from '../../components/ui'
import { queryKeys } from '../../hooks/queryKeys'
import { useToast } from '../../hooks/useToast'
import { membershipPlanApi } from '../../services/api/membershipApi'
import type { MembershipPlanDto } from '../../types'
import { getFriendlyErrorMessage } from '../../utils/apiError'
import { MembershipPlanForm } from './MembershipPlanForm'
import type { MembershipPlanFormValues } from './schemas'

const PAGE_SIZE = 20

export function MembershipPlansManager() {
  const { businessId } = useParams<{ businessId: string }>()
  const [page, setPage] = useState(0)
  const [modalMode, setModalMode] = useState<'create' | MembershipPlanDto | null>(null)
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const plansQuery = useQuery({
    queryKey: queryKeys.membershipPlans(businessId ?? '', page),
    queryFn: () => membershipPlanApi.list(businessId ?? '', { page, size: PAGE_SIZE }),
    enabled: Boolean(businessId),
  })

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ['businesses', businessId, 'membership-plans'] })

  const createMutation = useMutation({
    mutationFn: (values: MembershipPlanFormValues) => membershipPlanApi.create(businessId ?? '', values),
    onSuccess: () => {
      showToast('Membership plan added.', 'success')
      setModalMode(null)
      void invalidate()
    },
  })

  const updateMutation = useMutation({
    mutationFn: (vars: { id: string; values: MembershipPlanFormValues }) =>
      membershipPlanApi.update(businessId ?? '', vars.id, vars.values),
    onSuccess: () => {
      showToast('Membership plan updated.', 'success')
      setModalMode(null)
      void invalidate()
    },
  })

  const toggleStatusMutation = useMutation({
    mutationFn: async (plan: MembershipPlanDto): Promise<void> => {
      if (plan.status === 'ACTIVE') {
        await membershipPlanApi.remove(businessId ?? '', plan.id)
      } else {
        await membershipPlanApi.update(businessId ?? '', plan.id, { status: 'ACTIVE' })
      }
    },
    onSuccess: () => void invalidate(),
  })

  const editingPlan = modalMode && modalMode !== 'create' ? modalMode : undefined
  const isModalOpen = modalMode !== null

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-900">Membership plans</h2>
        <Button size="sm" onClick={() => setModalMode('create')}>
          Add plan
        </Button>
      </div>

      {plansQuery.isLoading && <LoadingSpinner label="Loading membership plans…" />}
      {plansQuery.isError && <ErrorState error={plansQuery.error} onRetry={() => plansQuery.refetch()} />}
      {plansQuery.isSuccess && plansQuery.data.content.length === 0 && (
        <EmptyState
          title="No membership plans yet"
          description="Add a plan so customers can purchase memberships."
          action={<Button onClick={() => setModalMode('create')}>Add plan</Button>}
        />
      )}
      {plansQuery.isSuccess && plansQuery.data.content.length > 0 && (
        <>
          <div className="flex flex-col gap-3">
            {plansQuery.data.content.map((plan) => (
              <MembershipPlanCard
                key={plan.id}
                plan={plan}
                action={
                  <div className="flex gap-2">
                    <Button variant="secondary" size="sm" onClick={() => setModalMode(plan)}>
                      Edit
                    </Button>
                    <Button
                      variant={plan.status === 'ACTIVE' ? 'danger' : 'secondary'}
                      size="sm"
                      isLoading={toggleStatusMutation.isPending && toggleStatusMutation.variables?.id === plan.id}
                      onClick={() => toggleStatusMutation.mutate(plan)}
                    >
                      {plan.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                    </Button>
                  </div>
                }
              />
            ))}
          </div>
          <Pagination page={plansQuery.data.page} totalPages={plansQuery.data.totalPages} onPageChange={setPage} />
        </>
      )}

      <Modal isOpen={isModalOpen} onClose={() => setModalMode(null)} title={editingPlan ? 'Edit plan' : 'Add plan'}>
        <MembershipPlanForm
          initialValue={editingPlan}
          isSubmitting={createMutation.isPending || updateMutation.isPending}
          submitError={
            createMutation.isError
              ? getFriendlyErrorMessage(createMutation.error)
              : updateMutation.isError
                ? getFriendlyErrorMessage(updateMutation.error)
                : null
          }
          submitLabel={editingPlan ? 'Save changes' : 'Add plan'}
          onSubmit={(values) =>
            editingPlan ? updateMutation.mutate({ id: editingPlan.id, values }) : createMutation.mutate(values)
          }
        />
      </Modal>
    </div>
  )
}

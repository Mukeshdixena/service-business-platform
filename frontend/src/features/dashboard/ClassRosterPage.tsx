import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { ClassEnrollmentStatusBadge } from '../../components/class/ClassEnrollmentStatusBadge'
import { Button, EmptyState, ErrorState, LoadingSpinner } from '../../components/ui'
import type { ButtonVariant } from '../../components/ui'
import { queryKeys } from '../../hooks/queryKeys'
import { useToast } from '../../hooks/useToast'
import { businessClassApi } from '../../services/api/classApi'
import type { ClassEnrollmentDto } from '../../types'
import { getFriendlyErrorMessage } from '../../utils/apiError'
import type { ClassRosterAction } from '../../utils/classEnrollmentTransitions'
import { getAvailableRosterActions } from '../../utils/classEnrollmentTransitions'

const TONE_TO_VARIANT: Record<'primary' | 'danger' | 'neutral', ButtonVariant> = {
  primary: 'primary',
  danger: 'danger',
  neutral: 'secondary',
}

export function ClassRosterPage() {
  const { businessId, classId } = useParams<{ businessId: string; classId: string }>()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const rosterQuery = useQuery({
    queryKey: queryKeys.classEnrollments(businessId ?? '', classId ?? ''),
    queryFn: () => businessClassApi.listEnrollments(businessId ?? '', classId ?? ''),
    enabled: Boolean(businessId && classId),
  })

  const actionMutation = useMutation({
    mutationFn: (vars: { enrollment: ClassEnrollmentDto; action: ClassRosterAction }) =>
      vars.action === 'attended'
        ? businessClassApi.markAttended(businessId ?? '', classId ?? '', vars.enrollment.id)
        : businessClassApi.markNoShow(businessId ?? '', classId ?? '', vars.enrollment.id),
    onSuccess: () => {
      showToast('Roster updated.', 'success')
      void queryClient.invalidateQueries({ queryKey: queryKeys.classEnrollments(businessId ?? '', classId ?? '') })
    },
    onError: (error: unknown) => showToast(getFriendlyErrorMessage(error), 'error'),
  })

  const enrollments = rosterQuery.data ?? []
  const active = enrollments.filter((entry) => entry.status === 'ENROLLED' || entry.status === 'WAITLISTED')
  const past = enrollments.filter((entry) => entry.status !== 'ENROLLED' && entry.status !== 'WAITLISTED')

  return (
    <div className="flex flex-col gap-4">
      <div>
        <Link to={`/dashboard/${businessId}/classes`} className="text-sm text-brand-600 hover:text-brand-700">
          &larr; Back to classes
        </Link>
        <h2 className="mt-1 text-lg font-semibold text-slate-900">Class roster</h2>
      </div>

      {rosterQuery.isLoading && <LoadingSpinner label="Loading roster…" />}
      {rosterQuery.isError && <ErrorState error={rosterQuery.error} onRetry={() => rosterQuery.refetch()} />}
      {rosterQuery.isSuccess && enrollments.length === 0 && (
        <EmptyState title="No enrollments yet" description="Customers who enroll in this class will appear here." />
      )}

      {rosterQuery.isSuccess && enrollments.length > 0 && (
        <div className="flex flex-col gap-3">
          {[...active, ...past].map((enrollment) => (
            <div
              key={enrollment.id}
              className="flex flex-col gap-3 rounded-lg border border-slate-200 bg-white p-4 shadow-sm sm:flex-row sm:items-center sm:justify-between"
            >
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <h3 className="font-medium text-slate-900">Customer #{enrollment.customerId.slice(0, 8)}</h3>
                  <ClassEnrollmentStatusBadge status={enrollment.status} />
                </div>
              </div>
              <div className="flex shrink-0 flex-wrap gap-2">
                {getAvailableRosterActions(enrollment.status).map((config) => (
                  <Button
                    key={config.action}
                    size="sm"
                    variant={TONE_TO_VARIANT[config.tone]}
                    isLoading={
                      actionMutation.isPending &&
                      actionMutation.variables?.enrollment.id === enrollment.id &&
                      actionMutation.variables.action === config.action
                    }
                    onClick={() => actionMutation.mutate({ enrollment, action: config.action })}
                  >
                    {config.label}
                  </Button>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

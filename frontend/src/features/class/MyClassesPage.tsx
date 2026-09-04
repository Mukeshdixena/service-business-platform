import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ClassEnrollmentStatusBadge } from '../../components/class/ClassEnrollmentStatusBadge'
import { Button, EmptyState, ErrorState, LoadingSpinner } from '../../components/ui'
import { useClassLookup } from '../../hooks/useEntityLookup'
import { queryKeys } from '../../hooks/queryKeys'
import { useToast } from '../../hooks/useToast'
import { customerClassApi } from '../../services/api/classApi'
import type { ClassEnrollmentDto } from '../../types'
import { getFriendlyErrorMessage } from '../../utils/apiError'
import { customerCanCancelEnrollment } from '../../utils/classEnrollmentTransitions'

function isActiveEnrollment(status: ClassEnrollmentDto['status']): boolean {
  return status === 'ENROLLED' || status === 'WAITLISTED'
}

export function MyClassesPage() {
  const lookup = useClassLookup()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const enrollmentsQuery = useQuery({
    queryKey: queryKeys.myClassEnrollments,
    queryFn: () => customerClassApi.listMine(),
  })

  const cancelMutation = useMutation({
    mutationFn: (enrollment: ClassEnrollmentDto) => {
      const businessId = lookup(enrollment.classId).businessId
      if (!businessId) {
        throw new Error(
          'Cannot resolve which business this class belongs to from cached data — revisit the business profile page first.',
        )
      }
      return customerClassApi.cancelEnrollment(businessId, enrollment.classId)
    },
    onSuccess: () => {
      showToast('Enrollment cancelled.', 'success')
      void queryClient.invalidateQueries({ queryKey: queryKeys.myClassEnrollments })
    },
    onError: (error: unknown) => showToast(getFriendlyErrorMessage(error), 'error'),
  })

  const enrollments = enrollmentsQuery.data ?? []
  const active = enrollments.filter((entry) => isActiveEnrollment(entry.status))
  const past = enrollments.filter((entry) => !isActiveEnrollment(entry.status))

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">My classes</h1>
        <p className="mt-1 text-sm text-slate-500">Classes you're enrolled in or waitlisted for.</p>
      </div>

      {enrollmentsQuery.isLoading && <LoadingSpinner label="Loading your classes…" />}
      {enrollmentsQuery.isError && <ErrorState error={enrollmentsQuery.error} onRetry={() => enrollmentsQuery.refetch()} />}
      {enrollmentsQuery.isSuccess && enrollments.length === 0 && (
        <EmptyState title="No classes yet" description="When you enroll in a class, it will show up here." />
      )}

      {enrollmentsQuery.isSuccess && active.length > 0 && (
        <div className="flex flex-col gap-3">
          {active.map((enrollment) => {
            const labels = lookup(enrollment.classId)
            return (
              <div
                key={enrollment.id}
                className="flex flex-col gap-3 rounded-lg border border-slate-200 bg-white p-4 shadow-sm sm:flex-row sm:items-center sm:justify-between"
              >
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="font-medium text-slate-900">
                      {labels.className ?? `Class #${enrollment.classId.slice(0, 8)}`}
                    </h3>
                    <ClassEnrollmentStatusBadge status={enrollment.status} />
                  </div>
                  {labels.businessName && <p className="text-sm text-slate-500">{labels.businessName}</p>}
                  {enrollment.status === 'WAITLISTED' && (
                    <p className="mt-1 text-sm text-amber-700">
                      This class is full — you&apos;ll be enrolled automatically if a spot opens up.
                    </p>
                  )}
                </div>
                {customerCanCancelEnrollment(enrollment.status) && (
                  <Button
                    variant="danger"
                    size="sm"
                    isLoading={cancelMutation.isPending && cancelMutation.variables?.id === enrollment.id}
                    onClick={() => cancelMutation.mutate(enrollment)}
                  >
                    Cancel
                  </Button>
                )}
              </div>
            )
          })}
        </div>
      )}

      {enrollmentsQuery.isSuccess && past.length > 0 && (
        <div className="flex flex-col gap-3">
          <h2 className="text-sm font-semibold text-slate-500">Past</h2>
          {past.map((enrollment) => {
            const labels = lookup(enrollment.classId)
            return (
              <div key={enrollment.id} className="flex flex-col gap-1 rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
                <div className="flex flex-wrap items-center gap-2">
                  <h3 className="font-medium text-slate-900">
                    {labels.className ?? `Class #${enrollment.classId.slice(0, 8)}`}
                  </h3>
                  <ClassEnrollmentStatusBadge status={enrollment.status} />
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

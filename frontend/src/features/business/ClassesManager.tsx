import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ClassCard } from '../../components/class/ClassCard'
import { Button, EmptyState, ErrorState, LoadingSpinner, Modal, Pagination } from '../../components/ui'
import { queryKeys } from '../../hooks/queryKeys'
import { useToast } from '../../hooks/useToast'
import { businessClassApi } from '../../services/api/classApi'
import { staffApi } from '../../services/api/staffApi'
import type { ClassDto, CreateClassRequest, UpdateClassRequest } from '../../types'
import { getFriendlyErrorMessage } from '../../utils/apiError'
import { ClassForm, localInputValueToIso } from './ClassForm'
import type { ClassFormValues } from './schemas'

const PAGE_SIZE = 20

function toRequest(values: ClassFormValues): CreateClassRequest {
  return {
    name: values.name,
    description: values.description || null,
    staffId: values.staffId || null,
    startAt: localInputValueToIso(values.startAt),
    endAt: localInputValueToIso(values.endAt),
    capacity: values.capacity,
    status: 'SCHEDULED',
  }
}

export function ClassesManager() {
  const { businessId } = useParams<{ businessId: string }>()
  const [page, setPage] = useState(0)
  const [modalMode, setModalMode] = useState<'create' | ClassDto | null>(null)
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const classesQuery = useQuery({
    queryKey: queryKeys.businessClasses(businessId ?? '', { page, size: PAGE_SIZE }),
    queryFn: () => businessClassApi.list(businessId ?? '', { page, size: PAGE_SIZE }),
    enabled: Boolean(businessId),
  })

  const staffQuery = useQuery({
    queryKey: queryKeys.staff(businessId ?? '', 0),
    queryFn: () => staffApi.list(businessId ?? '', { page: 0, size: 100 }),
    enabled: Boolean(businessId),
  })
  const staffById = new Map((staffQuery.data?.content ?? []).map((staff) => [staff.id, staff.displayName]))

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['businesses', businessId, 'classes'] })

  const createMutation = useMutation({
    mutationFn: (values: ClassFormValues) => businessClassApi.create(businessId ?? '', toRequest(values)),
    onSuccess: () => {
      showToast('Class added.', 'success')
      setModalMode(null)
      void invalidate()
    },
  })

  const updateMutation = useMutation({
    mutationFn: (vars: { id: string; values: ClassFormValues }) =>
      businessClassApi.update(businessId ?? '', vars.id, toRequest(vars.values) as UpdateClassRequest),
    onSuccess: () => {
      showToast('Class updated.', 'success')
      setModalMode(null)
      void invalidate()
    },
  })

  const cancelMutation = useMutation({
    mutationFn: (classItem: ClassDto) => businessClassApi.remove(businessId ?? '', classItem.id),
    onSuccess: () => {
      showToast('Class cancelled.', 'success')
      void invalidate()
    },
    onError: (error: unknown) => showToast(getFriendlyErrorMessage(error), 'error'),
  })

  const editingClass = modalMode && modalMode !== 'create' ? modalMode : undefined
  const isModalOpen = modalMode !== null

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-900">Classes</h2>
        <Button size="sm" onClick={() => setModalMode('create')}>
          Add class
        </Button>
      </div>

      {classesQuery.isLoading && <LoadingSpinner label="Loading classes…" />}
      {classesQuery.isError && <ErrorState error={classesQuery.error} onRetry={() => classesQuery.refetch()} />}
      {classesQuery.isSuccess && classesQuery.data.content.length === 0 && (
        <EmptyState
          title="No classes yet"
          description="Schedule a class so customers can enroll."
          action={<Button onClick={() => setModalMode('create')}>Add class</Button>}
        />
      )}
      {classesQuery.isSuccess && classesQuery.data.content.length > 0 && (
        <>
          <div className="flex flex-col gap-3">
            {classesQuery.data.content.map((classItem) => (
              <ClassCard
                key={classItem.id}
                classItem={classItem}
                staffLabel={classItem.staffId ? staffById.get(classItem.staffId) : undefined}
                action={
                  <div className="flex flex-wrap gap-2">
                    <Link to={`/dashboard/${businessId}/classes/${classItem.id}/roster`}>
                      <Button variant="secondary" size="sm">
                        Roster
                      </Button>
                    </Link>
                    {classItem.status === 'SCHEDULED' && (
                      <>
                        <Button variant="secondary" size="sm" onClick={() => setModalMode(classItem)}>
                          Edit
                        </Button>
                        <Button
                          variant="danger"
                          size="sm"
                          isLoading={cancelMutation.isPending && cancelMutation.variables?.id === classItem.id}
                          onClick={() => cancelMutation.mutate(classItem)}
                        >
                          Cancel
                        </Button>
                      </>
                    )}
                  </div>
                }
              />
            ))}
          </div>
          <Pagination page={classesQuery.data.page} totalPages={classesQuery.data.totalPages} onPageChange={setPage} />
        </>
      )}

      <Modal isOpen={isModalOpen} onClose={() => setModalMode(null)} title={editingClass ? 'Edit class' : 'Add class'}>
        <ClassForm
          initialValue={editingClass}
          staffOptions={staffQuery.data?.content ?? []}
          isSubmitting={createMutation.isPending || updateMutation.isPending}
          submitError={
            createMutation.isError
              ? getFriendlyErrorMessage(createMutation.error)
              : updateMutation.isError
                ? getFriendlyErrorMessage(updateMutation.error)
                : null
          }
          submitLabel={editingClass ? 'Save changes' : 'Add class'}
          onSubmit={(values) =>
            editingClass ? updateMutation.mutate({ id: editingClass.id, values }) : createMutation.mutate(values)
          }
        />
      </Modal>
    </div>
  )
}

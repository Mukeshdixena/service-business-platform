import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { StaffCard } from '../../components/business/StaffCard'
import { Button, EmptyState, ErrorState, LoadingSpinner, Modal, Pagination } from '../../components/ui'
import { queryKeys } from '../../hooks/queryKeys'
import { useToast } from '../../hooks/useToast'
import { catalogApi } from '../../services/api/catalogApi'
import { staffApi } from '../../services/api/staffApi'
import type { StaffDto } from '../../types'
import { getFriendlyErrorMessage } from '../../utils/apiError'
import { StaffForm } from './StaffForm'
import type { StaffFormValues } from './schemas'

const PAGE_SIZE = 20
/** Services list used to populate the "assign to staff" checklist — a generous single page covers the MVP scale. */
const SERVICES_LOOKUP_SIZE = 100

export function StaffManager() {
  const { businessId } = useParams<{ businessId: string }>()
  const [page, setPage] = useState(0)
  const [modalMode, setModalMode] = useState<'create' | StaffDto | null>(null)
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const staffQuery = useQuery({
    queryKey: queryKeys.staff(businessId ?? '', page),
    queryFn: () => staffApi.list(businessId ?? '', { page, size: PAGE_SIZE }),
    enabled: Boolean(businessId),
  })

  const servicesForAssignment = useQuery({
    queryKey: queryKeys.services(businessId ?? '', 0),
    queryFn: () => catalogApi.list(businessId ?? '', { page: 0, size: SERVICES_LOOKUP_SIZE }),
    enabled: Boolean(businessId),
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['businesses', businessId, 'staff'] })

  const createMutation = useMutation({
    mutationFn: (values: StaffFormValues) => staffApi.create(businessId ?? '', values),
    onSuccess: () => {
      showToast('Staff member added.', 'success')
      setModalMode(null)
      void invalidate()
    },
  })

  const updateMutation = useMutation({
    mutationFn: (vars: { id: string; values: StaffFormValues }) => staffApi.update(businessId ?? '', vars.id, vars.values),
    onSuccess: () => {
      showToast('Staff member updated.', 'success')
      setModalMode(null)
      void invalidate()
    },
  })

  const toggleStatusMutation = useMutation({
    mutationFn: async (staff: StaffDto): Promise<void> => {
      if (staff.status === 'ACTIVE') {
        await staffApi.remove(businessId ?? '', staff.id)
      } else {
        await staffApi.update(businessId ?? '', staff.id, { status: 'ACTIVE' })
      }
    },
    onSuccess: () => void invalidate(),
  })

  const editingStaff = modalMode && modalMode !== 'create' ? modalMode : undefined
  const activeServices = (servicesForAssignment.data?.content ?? []).filter((service) => service.status === 'ACTIVE')

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-900">Staff</h2>
        <Button size="sm" onClick={() => setModalMode('create')}>
          Add staff
        </Button>
      </div>

      {staffQuery.isLoading && <LoadingSpinner label="Loading staff…" />}
      {staffQuery.isError && <ErrorState error={staffQuery.error} onRetry={() => staffQuery.refetch()} />}
      {staffQuery.isSuccess && staffQuery.data.content.length === 0 && (
        <EmptyState
          title="No staff yet"
          description="Add staff so customers can choose who they book with."
          action={<Button onClick={() => setModalMode('create')}>Add staff</Button>}
        />
      )}
      {staffQuery.isSuccess && staffQuery.data.content.length > 0 && (
        <>
          <div className="flex flex-col gap-3">
            {staffQuery.data.content.map((staff) => (
              <StaffCard
                key={staff.id}
                staff={staff}
                action={
                  <div className="flex gap-2">
                    <Button variant="secondary" size="sm" onClick={() => setModalMode(staff)}>
                      Edit
                    </Button>
                    <Button
                      variant={staff.status === 'ACTIVE' ? 'danger' : 'secondary'}
                      size="sm"
                      isLoading={toggleStatusMutation.isPending && toggleStatusMutation.variables?.id === staff.id}
                      onClick={() => toggleStatusMutation.mutate(staff)}
                    >
                      {staff.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                    </Button>
                  </div>
                }
              />
            ))}
          </div>
          <Pagination page={staffQuery.data.page} totalPages={staffQuery.data.totalPages} onPageChange={setPage} />
        </>
      )}

      <Modal isOpen={modalMode !== null} onClose={() => setModalMode(null)} title={editingStaff ? 'Edit staff' : 'Add staff'}>
        <StaffForm
          initialValue={editingStaff}
          availableServices={activeServices}
          isSubmitting={createMutation.isPending || updateMutation.isPending}
          submitError={
            createMutation.isError
              ? getFriendlyErrorMessage(createMutation.error)
              : updateMutation.isError
                ? getFriendlyErrorMessage(updateMutation.error)
                : null
          }
          submitLabel={editingStaff ? 'Save changes' : 'Add staff'}
          onSubmit={(values) =>
            editingStaff ? updateMutation.mutate({ id: editingStaff.id, values }) : createMutation.mutate(values)
          }
        />
      </Modal>
    </div>
  )
}

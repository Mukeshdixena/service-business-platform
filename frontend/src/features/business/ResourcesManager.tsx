import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { ResourceCard } from '../../components/resource/ResourceCard'
import { Button, EmptyState, ErrorState, LoadingSpinner, Modal, Pagination } from '../../components/ui'
import { queryKeys } from '../../hooks/queryKeys'
import { useToast } from '../../hooks/useToast'
import { resourceApi } from '../../services/api/resourceApi'
import type { ResourceDto } from '../../types'
import { getFriendlyErrorMessage } from '../../utils/apiError'
import { ResourceForm } from './ResourceForm'
import type { ResourceFormValues } from './schemas'

const PAGE_SIZE = 20

export function ResourcesManager() {
  const { businessId } = useParams<{ businessId: string }>()
  const [page, setPage] = useState(0)
  const [modalMode, setModalMode] = useState<'create' | ResourceDto | null>(null)
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const resourcesQuery = useQuery({
    queryKey: queryKeys.businessResources(businessId ?? '', page),
    queryFn: () => resourceApi.list(businessId ?? '', { page, size: PAGE_SIZE }),
    enabled: Boolean(businessId),
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['businesses', businessId, 'resources'] })

  const createMutation = useMutation({
    mutationFn: (values: ResourceFormValues) => resourceApi.create(businessId ?? '', values),
    onSuccess: () => {
      showToast('Resource added.', 'success')
      setModalMode(null)
      void invalidate()
    },
  })

  const updateMutation = useMutation({
    mutationFn: (vars: { id: string; values: ResourceFormValues }) =>
      resourceApi.update(businessId ?? '', vars.id, vars.values),
    onSuccess: () => {
      showToast('Resource updated.', 'success')
      setModalMode(null)
      void invalidate()
    },
  })

  const toggleStatusMutation = useMutation({
    mutationFn: async (resource: ResourceDto): Promise<void> => {
      if (resource.status === 'UNAVAILABLE') {
        await resourceApi.update(businessId ?? '', resource.id, { status: 'AVAILABLE' })
      } else {
        await resourceApi.remove(businessId ?? '', resource.id)
      }
    },
    onSuccess: () => void invalidate(),
    onError: (error: unknown) => showToast(getFriendlyErrorMessage(error), 'error'),
  })

  const editingResource = modalMode && modalMode !== 'create' ? modalMode : undefined
  const isModalOpen = modalMode !== null

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-900">Resources</h2>
        <Button size="sm" onClick={() => setModalMode('create')}>
          Add resource
        </Button>
      </div>

      {resourcesQuery.isLoading && <LoadingSpinner label="Loading resources…" />}
      {resourcesQuery.isError && <ErrorState error={resourcesQuery.error} onRetry={() => resourcesQuery.refetch()} />}
      {resourcesQuery.isSuccess && resourcesQuery.data.content.length === 0 && (
        <EmptyState
          title="No resources yet"
          description="Add rentable/bookable resources (vehicles, equipment, rooms) so customers can rent them."
          action={<Button onClick={() => setModalMode('create')}>Add resource</Button>}
        />
      )}
      {resourcesQuery.isSuccess && resourcesQuery.data.content.length > 0 && (
        <>
          <div className="flex flex-col gap-3">
            {resourcesQuery.data.content.map((resource) => (
              <ResourceCard
                key={resource.id}
                resource={resource}
                action={
                  <div className="flex gap-2">
                    <Button variant="secondary" size="sm" onClick={() => setModalMode(resource)}>
                      Edit
                    </Button>
                    <Button
                      variant={resource.status === 'UNAVAILABLE' ? 'secondary' : 'danger'}
                      size="sm"
                      isLoading={toggleStatusMutation.isPending && toggleStatusMutation.variables?.id === resource.id}
                      onClick={() => toggleStatusMutation.mutate(resource)}
                    >
                      {resource.status === 'UNAVAILABLE' ? 'Make available' : 'Remove'}
                    </Button>
                  </div>
                }
              />
            ))}
          </div>
          <Pagination
            page={resourcesQuery.data.page}
            totalPages={resourcesQuery.data.totalPages}
            onPageChange={setPage}
          />
        </>
      )}

      <Modal
        isOpen={isModalOpen}
        onClose={() => setModalMode(null)}
        title={editingResource ? 'Edit resource' : 'Add resource'}
      >
        <ResourceForm
          initialValue={editingResource}
          isSubmitting={createMutation.isPending || updateMutation.isPending}
          submitError={
            createMutation.isError
              ? getFriendlyErrorMessage(createMutation.error)
              : updateMutation.isError
                ? getFriendlyErrorMessage(updateMutation.error)
                : null
          }
          submitLabel={editingResource ? 'Save changes' : 'Add resource'}
          onSubmit={(values) =>
            editingResource ? updateMutation.mutate({ id: editingResource.id, values }) : createMutation.mutate(values)
          }
        />
      </Modal>
    </div>
  )
}

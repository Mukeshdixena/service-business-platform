import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { ServiceCard } from '../../components/business/ServiceCard'
import { Button, EmptyState, ErrorState, LoadingSpinner, Modal, Pagination } from '../../components/ui'
import { queryKeys } from '../../hooks/queryKeys'
import { useToast } from '../../hooks/useToast'
import { catalogApi } from '../../services/api/catalogApi'
import type { ServiceDto } from '../../types'
import { getFriendlyErrorMessage } from '../../utils/apiError'
import { ServiceForm } from './ServiceForm'
import type { ServiceFormValues } from './schemas'

const PAGE_SIZE = 20

export function ServicesManager() {
  const { businessId } = useParams<{ businessId: string }>()
  const [page, setPage] = useState(0)
  const [modalMode, setModalMode] = useState<'create' | ServiceDto | null>(null)
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const servicesQuery = useQuery({
    queryKey: queryKeys.services(businessId ?? '', page),
    queryFn: () => catalogApi.list(businessId ?? '', { page, size: PAGE_SIZE }),
    enabled: Boolean(businessId),
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['businesses', businessId, 'services'] })

  const createMutation = useMutation({
    mutationFn: (values: ServiceFormValues) => catalogApi.create(businessId ?? '', values),
    onSuccess: () => {
      showToast('Service added.', 'success')
      setModalMode(null)
      void invalidate()
    },
  })

  const updateMutation = useMutation({
    mutationFn: (vars: { id: string; values: ServiceFormValues }) =>
      catalogApi.update(businessId ?? '', vars.id, vars.values),
    onSuccess: () => {
      showToast('Service updated.', 'success')
      setModalMode(null)
      void invalidate()
    },
  })

  const toggleStatusMutation = useMutation({
    mutationFn: async (service: ServiceDto): Promise<void> => {
      if (service.status === 'ACTIVE') {
        await catalogApi.remove(businessId ?? '', service.id)
      } else {
        await catalogApi.update(businessId ?? '', service.id, { status: 'ACTIVE' })
      }
    },
    onSuccess: () => void invalidate(),
  })

  const editingService = modalMode && modalMode !== 'create' ? modalMode : undefined
  const isModalOpen = modalMode !== null

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-900">Services</h2>
        <Button size="sm" onClick={() => setModalMode('create')}>
          Add service
        </Button>
      </div>

      {servicesQuery.isLoading && <LoadingSpinner label="Loading services…" />}
      {servicesQuery.isError && <ErrorState error={servicesQuery.error} onRetry={() => servicesQuery.refetch()} />}
      {servicesQuery.isSuccess && servicesQuery.data.content.length === 0 && (
        <EmptyState
          title="No services yet"
          description="Add your first service so customers can start booking."
          action={<Button onClick={() => setModalMode('create')}>Add service</Button>}
        />
      )}
      {servicesQuery.isSuccess && servicesQuery.data.content.length > 0 && (
        <>
          <div className="flex flex-col gap-3">
            {servicesQuery.data.content.map((service) => (
              <ServiceCard
                key={service.id}
                service={service}
                action={
                  <div className="flex gap-2">
                    <Button variant="secondary" size="sm" onClick={() => setModalMode(service)}>
                      Edit
                    </Button>
                    <Button
                      variant={service.status === 'ACTIVE' ? 'danger' : 'secondary'}
                      size="sm"
                      isLoading={toggleStatusMutation.isPending && toggleStatusMutation.variables?.id === service.id}
                      onClick={() => toggleStatusMutation.mutate(service)}
                    >
                      {service.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                    </Button>
                  </div>
                }
              />
            ))}
          </div>
          <Pagination page={servicesQuery.data.page} totalPages={servicesQuery.data.totalPages} onPageChange={setPage} />
        </>
      )}

      <Modal isOpen={isModalOpen} onClose={() => setModalMode(null)} title={editingService ? 'Edit service' : 'Add service'}>
        <ServiceForm
          initialValue={editingService}
          isSubmitting={createMutation.isPending || updateMutation.isPending}
          submitError={
            createMutation.isError
              ? getFriendlyErrorMessage(createMutation.error)
              : updateMutation.isError
                ? getFriendlyErrorMessage(updateMutation.error)
                : null
          }
          submitLabel={editingService ? 'Save changes' : 'Add service'}
          onSubmit={(values) =>
            editingService ? updateMutation.mutate({ id: editingService.id, values }) : createMutation.mutate(values)
          }
        />
      </Modal>
    </div>
  )
}

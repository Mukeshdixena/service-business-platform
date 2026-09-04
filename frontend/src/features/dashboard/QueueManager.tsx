import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { QueueEntryCard } from '../../components/queue/QueueEntryCard'
import { Button, EmptyState, ErrorState, LoadingSpinner } from '../../components/ui'
import type { ButtonVariant } from '../../components/ui'
import { queryKeys } from '../../hooks/queryKeys'
import { useToast } from '../../hooks/useToast'
import { businessQueueApi } from '../../services/api/queueApi'
import { catalogApi } from '../../services/api/catalogApi'
import { staffApi } from '../../services/api/staffApi'
import type { QueueEntryDto } from '../../types'
import { getFriendlyErrorMessage } from '../../utils/apiError'
import type { QueueAction } from '../../utils/queueTransitions'
import { getAvailableQueueActions } from '../../utils/queueTransitions'

const POLL_INTERVAL_MS = 7000

const TONE_TO_VARIANT: Record<'primary' | 'danger' | 'neutral', ButtonVariant> = {
  primary: 'primary',
  danger: 'danger',
  neutral: 'secondary',
}

function runAction(businessId: string, entry: QueueEntryDto, action: QueueAction) {
  switch (action) {
    case 'call':
      return businessQueueApi.call(businessId, entry.id)
    case 'start':
      return businessQueueApi.start(businessId, entry.id)
    case 'complete':
      return businessQueueApi.complete(businessId, entry.id)
    case 'skip':
      return businessQueueApi.skip(businessId, entry.id)
    case 'no-show':
      return businessQueueApi.markNoShow(businessId, entry.id)
    case 'cancel':
      return businessQueueApi.cancel(businessId, entry.id)
  }
}

export function QueueManager() {
  const { businessId } = useParams<{ businessId: string }>()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const queueQuery = useQuery({
    queryKey: queryKeys.businessQueue(businessId ?? ''),
    queryFn: () => businessQueueApi.list(businessId ?? ''),
    enabled: Boolean(businessId),
    refetchInterval: POLL_INTERVAL_MS,
  })

  const servicesQuery = useQuery({
    queryKey: queryKeys.services(businessId ?? '', 0),
    queryFn: () => catalogApi.list(businessId ?? '', { page: 0, size: 100 }),
    enabled: Boolean(businessId),
  })
  const staffQuery = useQuery({
    queryKey: queryKeys.staff(businessId ?? '', 0),
    queryFn: () => staffApi.list(businessId ?? '', { page: 0, size: 100 }),
    enabled: Boolean(businessId),
  })
  const serviceNameById = new Map((servicesQuery.data?.content ?? []).map((service) => [service.id, service.name]))
  const staffNameById = new Map((staffQuery.data?.content ?? []).map((staff) => [staff.id, staff.displayName]))

  const invalidateQueue = () => queryClient.invalidateQueries({ queryKey: ['businesses', businessId, 'queue'] })

  const actionMutation = useMutation({
    mutationFn: (vars: { entry: QueueEntryDto; action: QueueAction }) =>
      runAction(businessId ?? '', vars.entry, vars.action),
    onSuccess: () => {
      showToast('Queue updated.', 'success')
      void invalidateQueue()
    },
    onError: (error: unknown) => showToast(getFriendlyErrorMessage(error), 'error'),
  })

  const entries = queueQuery.data ?? []
  const orderedEntries = [...entries].sort((a, b) => a.position - b.position)

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-900">Queue</h2>
      </div>

      {queueQuery.isLoading && <LoadingSpinner label="Loading queue…" />}
      {queueQuery.isError && <ErrorState error={queueQuery.error} onRetry={() => queueQuery.refetch()} />}
      {queueQuery.isSuccess && orderedEntries.length === 0 && (
        <EmptyState title="Queue is empty" description="Customers who join the queue will appear here." />
      )}

      {queueQuery.isSuccess && orderedEntries.length > 0 && (
        <div className="flex flex-col gap-3">
          {orderedEntries.map((entry) => (
            <QueueEntryCard
              key={entry.id}
              entry={entry}
              serviceLabel={serviceNameById.get(entry.serviceId)}
              staffLabel={entry.staffId ? staffNameById.get(entry.staffId) : undefined}
              actions={getAvailableQueueActions(entry.status).map((config) => (
                <Button
                  key={config.action}
                  size="sm"
                  variant={TONE_TO_VARIANT[config.tone]}
                  isLoading={
                    actionMutation.isPending &&
                    actionMutation.variables?.entry.id === entry.id &&
                    actionMutation.variables.action === config.action
                  }
                  onClick={() => actionMutation.mutate({ entry, action: config.action })}
                >
                  {config.label}
                </Button>
              ))}
            />
          ))}
        </div>
      )}
    </div>
  )
}

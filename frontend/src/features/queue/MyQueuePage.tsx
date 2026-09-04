import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button, EmptyState, ErrorState, LoadingSpinner } from '../../components/ui'
import { QueueStatusBadge } from '../../components/queue/QueueStatusBadge'
import { useEntityLookup } from '../../hooks/useEntityLookup'
import { queryKeys } from '../../hooks/queryKeys'
import { useToast } from '../../hooks/useToast'
import { customerQueueApi } from '../../services/api/queueApi'
import type { QueueEntryDto } from '../../types'
import { formatDateTime } from '../../utils/date'
import { customerCanCancelQueueEntry, isActiveQueueStatus } from '../../utils/queueTransitions'

const POLL_INTERVAL_MS = 7000

function LiveQueueStatus({ entry }: { entry: QueueEntryDto }) {
  const statusQuery = useQuery({
    queryKey: queryKeys.queueEntryStatus(entry.id),
    queryFn: () => customerQueueApi.getStatus(entry.id),
    refetchInterval: POLL_INTERVAL_MS,
  })

  if (statusQuery.isLoading) return <p className="text-sm text-slate-500">Checking your position…</p>
  if (statusQuery.isError || !statusQuery.data) return null

  return (
    <p className="mt-1 text-sm text-slate-600">
      {statusQuery.data.peopleAhead} {statusQuery.data.peopleAhead === 1 ? 'person' : 'people'} ahead of you
      {statusQuery.data.estimatedWaitMinutes != null && ` · ~${statusQuery.data.estimatedWaitMinutes} min wait`}
    </p>
  )
}

export function MyQueuePage() {
  const lookup = useEntityLookup()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const queueQuery = useQuery({
    queryKey: queryKeys.myQueueEntries,
    queryFn: () => customerQueueApi.listMine(),
    refetchInterval: POLL_INTERVAL_MS,
  })

  const cancelMutation = useMutation({
    mutationFn: (entry: QueueEntryDto) => customerQueueApi.cancel(entry.businessId, entry.id),
    onSuccess: () => {
      showToast('You left the queue.', 'success')
      void queryClient.invalidateQueries({ queryKey: queryKeys.myQueueEntries })
    },
  })

  const entries = queueQuery.data ?? []
  const activeEntries = entries.filter((entry) => isActiveQueueStatus(entry.status))
  const pastEntries = entries.filter((entry) => !isActiveQueueStatus(entry.status))

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">My queue</h1>
        <p className="mt-1 text-sm text-slate-500">Track where you stand in line, live.</p>
      </div>

      {queueQuery.isLoading && <LoadingSpinner label="Loading your queue status…" />}
      {queueQuery.isError && <ErrorState error={queueQuery.error} onRetry={() => queueQuery.refetch()} />}

      {queueQuery.isSuccess && entries.length === 0 && (
        <EmptyState title="You're not in any queue" description="When you join a queue, it will show up here." />
      )}

      {queueQuery.isSuccess && activeEntries.length > 0 && (
        <div className="flex flex-col gap-3">
          {activeEntries.map((entry) => {
            const labels = lookup(entry.businessId, entry.serviceId)
            return (
              <div key={entry.id} className="flex flex-col gap-3 rounded-lg border border-slate-200 bg-white p-4 shadow-sm sm:flex-row sm:items-center sm:justify-between">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="font-medium text-slate-900">{labels.serviceName ?? `Service #${entry.serviceId.slice(0, 8)}`}</h3>
                    <QueueStatusBadge status={entry.status} />
                  </div>
                  {labels.businessName && <p className="text-sm text-slate-500">{labels.businessName}</p>}
                  <p className="mt-1 text-sm text-slate-600">Joined {formatDateTime(entry.joinedAt)}</p>
                  <LiveQueueStatus entry={entry} />
                </div>
                {customerCanCancelQueueEntry(entry.status) && (
                  <Button
                    variant="danger"
                    size="sm"
                    isLoading={cancelMutation.isPending && cancelMutation.variables?.id === entry.id}
                    onClick={() => cancelMutation.mutate(entry)}
                  >
                    Leave queue
                  </Button>
                )}
              </div>
            )
          })}
        </div>
      )}

      {queueQuery.isSuccess && pastEntries.length > 0 && (
        <div className="flex flex-col gap-3">
          <h2 className="text-sm font-semibold text-slate-500">Past</h2>
          {pastEntries.map((entry) => {
            const labels = lookup(entry.businessId, entry.serviceId)
            return (
              <div key={entry.id} className="flex flex-col gap-1 rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
                <div className="flex flex-wrap items-center gap-2">
                  <h3 className="font-medium text-slate-900">{labels.serviceName ?? `Service #${entry.serviceId.slice(0, 8)}`}</h3>
                  <QueueStatusBadge status={entry.status} />
                </div>
                <p className="text-sm text-slate-500">{formatDateTime(entry.joinedAt)}</p>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

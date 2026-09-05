import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { NotificationCard } from '../../components/notification/NotificationCard'
import { Button, EmptyState, ErrorState, LoadingSpinner, Pagination } from '../../components/ui'
import { notificationApi } from '../../services/api/notificationApi'
import { useState } from 'react'

const PAGE_SIZE = 20

export function NotificationsPage() {
  const [page, setPage] = useState(0)
  const queryClient = useQueryClient()

  const notificationsQuery = useQuery({
    queryKey: ['me', 'notifications', { page, size: PAGE_SIZE }],
    queryFn: () => notificationApi.list({ page, size: PAGE_SIZE }),
  })

  const markAllReadMutation = useMutation({
    mutationFn: () => notificationApi.markAllAsRead(),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['me', 'notifications'] })
    },
  })

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Notifications</h1>
          <p className="mt-1 text-sm text-slate-500">Stay updated on your bookings and memberships.</p>
        </div>
        {notificationsQuery.isSuccess && notificationsQuery.data.content.some((n) => !n.isRead) && (
          <Button
            variant="secondary"
            size="sm"
            isLoading={markAllReadMutation.isPending}
            onClick={() => markAllReadMutation.mutate()}
          >
            Mark all as read
          </Button>
        )}
      </div>

      {notificationsQuery.isLoading && <LoadingSpinner label="Loading notifications…" />}
      {notificationsQuery.isError && (
        <ErrorState error={notificationsQuery.error} onRetry={() => notificationsQuery.refetch()} />
      )}
      {notificationsQuery.isSuccess && notificationsQuery.data.content.length === 0 && (
        <EmptyState title="No notifications" description="You're all caught up!" />
      )}

      {notificationsQuery.isSuccess && notificationsQuery.data.content.length > 0 && (
        <>
          <div className="flex flex-col gap-3">
            {notificationsQuery.data.content.map((notification) => (
              <NotificationCard key={notification.id} notification={notification} />
            ))}
          </div>
          <Pagination
            page={notificationsQuery.data.page}
            totalPages={notificationsQuery.data.totalPages}
            onPageChange={setPage}
          />
        </>
      )}
    </div>
  )
}

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { BookingCard } from '../../components/booking/BookingCard'
import { Button, EmptyState, ErrorState, FormError, LoadingSpinner, Modal, Pagination, Select, TextArea } from '../../components/ui'
import { useEntityLookup } from '../../hooks/useEntityLookup'
import { useToast } from '../../hooks/useToast'
import { customerBookingApi } from '../../services/api/bookingApi'
import { BOOKING_STATUSES } from '../../types'
import type { BookingDto, BookingStatus } from '../../types'
import { getFriendlyErrorMessage } from '../../utils/apiError'
import { customerCanCancel } from '../../utils/bookingTransitions'

const PAGE_SIZE = 10

const STATUS_OPTIONS = [
  { value: '', label: 'All statuses' },
  ...BOOKING_STATUSES.map((status) => ({ value: status, label: status.replace('_', ' ') })),
]

export function MyBookingsPage() {
  const [statusFilter, setStatusFilter] = useState<BookingStatus | ''>('')
  const [page, setPage] = useState(0)
  const [cancelTarget, setCancelTarget] = useState<BookingDto | null>(null)
  const [cancelReason, setCancelReason] = useState('')

  const lookup = useEntityLookup()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const params = { status: statusFilter || undefined, page, size: PAGE_SIZE }

  const bookingsQuery = useQuery({
    queryKey: ['me', 'bookings', params],
    queryFn: () => customerBookingApi.listMine(params),
  })

  const cancelMutation = useMutation({
    mutationFn: (vars: { id: string; reason: string }) => customerBookingApi.cancel(vars.id, vars.reason),
    onSuccess: () => {
      showToast('Booking cancelled.', 'success')
      setCancelTarget(null)
      setCancelReason('')
      void queryClient.invalidateQueries({ queryKey: ['me', 'bookings'] })
    },
  })

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">My bookings</h1>
          <p className="mt-1 text-sm text-slate-500">Track and manage your upcoming and past appointments.</p>
        </div>
        <div className="w-48">
          <Select
            options={STATUS_OPTIONS}
            value={statusFilter}
            onChange={(event) => {
              setStatusFilter(event.target.value as BookingStatus | '')
              setPage(0)
            }}
          />
        </div>
      </div>

      {bookingsQuery.isLoading && <LoadingSpinner label="Loading your bookings…" />}

      {bookingsQuery.isError && <ErrorState error={bookingsQuery.error} onRetry={() => bookingsQuery.refetch()} />}

      {bookingsQuery.isSuccess && bookingsQuery.data.content.length === 0 && (
        <EmptyState title="No bookings yet" description="When you book an appointment, it will show up here." />
      )}

      {bookingsQuery.isSuccess && bookingsQuery.data.content.length > 0 && (
        <>
          <div className="flex flex-col gap-3">
            {bookingsQuery.data.content.map((booking) => {
              const labels = lookup(booking.businessId, booking.serviceId, booking.resourceId)
              return (
                <BookingCard
                  key={booking.id}
                  booking={booking}
                  businessLabel={labels.businessName}
                  serviceLabel={labels.serviceName}
                  resourceLabel={labels.resourceName}
                  actions={
                    customerCanCancel(booking.status) ? (
                      <Button variant="danger" size="sm" onClick={() => setCancelTarget(booking)}>
                        Cancel
                      </Button>
                    ) : undefined
                  }
                />
              )
            })}
          </div>
          <Pagination page={bookingsQuery.data.page} totalPages={bookingsQuery.data.totalPages} onPageChange={setPage} />
        </>
      )}

      <Modal
        isOpen={cancelTarget !== null}
        onClose={() => {
          setCancelTarget(null)
          setCancelReason('')
        }}
        title="Cancel booking"
        footer={
          <>
            <Button
              variant="secondary"
              onClick={() => {
                setCancelTarget(null)
                setCancelReason('')
              }}
            >
              Keep booking
            </Button>
            <Button
              variant="danger"
              isLoading={cancelMutation.isPending}
              disabled={cancelReason.trim().length === 0}
              onClick={() => cancelTarget && cancelMutation.mutate({ id: cancelTarget.id, reason: cancelReason.trim() })}
            >
              Confirm cancellation
            </Button>
          </>
        }
      >
        <div className="flex flex-col gap-3">
          <FormError message={cancelMutation.isError ? getFriendlyErrorMessage(cancelMutation.error) : null} />
          <p className="text-sm text-slate-600">Let the business know why you&apos;re cancelling.</p>
          <TextArea
            label="Reason"
            value={cancelReason}
            onChange={(event) => setCancelReason(event.target.value)}
            placeholder="e.g. Schedule conflict"
          />
        </div>
      </Modal>
    </div>
  )
}

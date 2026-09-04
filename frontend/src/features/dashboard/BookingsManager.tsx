import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { BookingCard } from '../../components/booking/BookingCard'
import { Button, EmptyState, ErrorState, FormError, LoadingSpinner, Modal, Pagination, Select, TextArea } from '../../components/ui'
import type { ButtonVariant } from '../../components/ui'
import { queryKeys } from '../../hooks/queryKeys'
import { useToast } from '../../hooks/useToast'
import { businessBookingApi } from '../../services/api/bookingApi'
import { catalogApi } from '../../services/api/catalogApi'
import { staffApi } from '../../services/api/staffApi'
import { BOOKING_STATUSES } from '../../types'
import type { BookingDto, BookingStatus } from '../../types'
import { getFriendlyErrorMessage } from '../../utils/apiError'
import type { BookingAction } from '../../utils/bookingTransitions'
import { getAvailableBookingActions } from '../../utils/bookingTransitions'

const PAGE_SIZE = 20
const REASON_REQUIRED_ACTIONS: BookingAction[] = ['cancel', 'reject']

const TONE_TO_VARIANT: Record<'primary' | 'danger' | 'neutral', ButtonVariant> = {
  primary: 'primary',
  danger: 'danger',
  neutral: 'secondary',
}

const STATUS_OPTIONS = [
  { value: '', label: 'All statuses' },
  ...BOOKING_STATUSES.map((status) => ({ value: status, label: status.replace('_', ' ') })),
]

function runAction(businessId: string, booking: BookingDto, action: BookingAction, reason?: string) {
  switch (action) {
    case 'confirm':
      return businessBookingApi.confirm(businessId, booking.id)
    case 'check-in':
      return businessBookingApi.checkIn(businessId, booking.id)
    case 'start':
      return businessBookingApi.start(businessId, booking.id)
    case 'complete':
      return businessBookingApi.complete(businessId, booking.id)
    case 'cancel':
      return businessBookingApi.cancel(businessId, booking.id, reason ?? '')
    case 'no-show':
      return businessBookingApi.markNoShow(businessId, booking.id)
    case 'reject':
      return businessBookingApi.reject(businessId, booking.id, reason ?? '')
  }
}

export function BookingsManager() {
  const { businessId } = useParams<{ businessId: string }>()
  const [statusFilter, setStatusFilter] = useState<BookingStatus | ''>('')
  const [page, setPage] = useState(0)
  const [reasonPrompt, setReasonPrompt] = useState<{ booking: BookingDto; action: BookingAction } | null>(null)
  const [reason, setReason] = useState('')
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const params = { status: statusFilter || undefined, page, size: PAGE_SIZE }

  const bookingsQuery = useQuery({
    queryKey: queryKeys.businessBookings(businessId ?? '', params),
    queryFn: () => businessBookingApi.list(businessId ?? '', params),
    enabled: Boolean(businessId),
  })

  // Owner-scoped services/staff lookups are authoritative (unlike the customer-side best-effort cache lookup).
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

  const invalidateBookings = () =>
    queryClient.invalidateQueries({ queryKey: ['businesses', businessId, 'bookings'] })

  const actionMutation = useMutation({
    mutationFn: (vars: { booking: BookingDto; action: BookingAction; reason?: string }) =>
      runAction(businessId ?? '', vars.booking, vars.action, vars.reason),
    onSuccess: () => {
      showToast('Booking updated.', 'success')
      setReasonPrompt(null)
      setReason('')
      void invalidateBookings()
    },
  })

  function handleAction(booking: BookingDto, action: BookingAction) {
    if (REASON_REQUIRED_ACTIONS.includes(action)) {
      setReasonPrompt({ booking, action })
    } else {
      actionMutation.mutate({ booking, action })
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h2 className="text-lg font-semibold text-slate-900">Bookings</h2>
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

      {bookingsQuery.isLoading && <LoadingSpinner label="Loading bookings…" />}
      {bookingsQuery.isError && <ErrorState error={bookingsQuery.error} onRetry={() => bookingsQuery.refetch()} />}
      {bookingsQuery.isSuccess && bookingsQuery.data.content.length === 0 && (
        <EmptyState title="No bookings" description="Bookings from customers will appear here." />
      )}

      {bookingsQuery.isSuccess && bookingsQuery.data.content.length > 0 && (
        <>
          <div className="flex flex-col gap-3">
            {bookingsQuery.data.content.map((booking) => (
              <BookingCard
                key={booking.id}
                booking={booking}
                serviceLabel={serviceNameById.get(booking.serviceId)}
                staffLabel={booking.staffId ? staffNameById.get(booking.staffId) : undefined}
                actions={getAvailableBookingActions(booking.status).map((config) => (
                  <Button
                    key={config.action}
                    size="sm"
                    variant={TONE_TO_VARIANT[config.tone]}
                    isLoading={
                      actionMutation.isPending &&
                      actionMutation.variables?.booking.id === booking.id &&
                      actionMutation.variables.action === config.action
                    }
                    onClick={() => handleAction(booking, config.action)}
                  >
                    {config.label}
                  </Button>
                ))}
              />
            ))}
          </div>
          <Pagination page={bookingsQuery.data.page} totalPages={bookingsQuery.data.totalPages} onPageChange={setPage} />
        </>
      )}

      <Modal
        isOpen={reasonPrompt !== null}
        onClose={() => {
          setReasonPrompt(null)
          setReason('')
        }}
        title={reasonPrompt?.action === 'reject' ? 'Reject booking' : 'Cancel booking'}
        footer={
          <>
            <Button
              variant="secondary"
              onClick={() => {
                setReasonPrompt(null)
                setReason('')
              }}
            >
              Back
            </Button>
            <Button
              variant="danger"
              isLoading={actionMutation.isPending}
              disabled={reason.trim().length === 0}
              onClick={() => reasonPrompt && actionMutation.mutate({ ...reasonPrompt, reason: reason.trim() })}
            >
              Confirm
            </Button>
          </>
        }
      >
        <div className="flex flex-col gap-3">
          <FormError message={actionMutation.isError ? getFriendlyErrorMessage(actionMutation.error) : null} />
          <TextArea
            label="Reason"
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            placeholder="Let the customer know why"
          />
        </div>
      </Modal>
    </div>
  )
}

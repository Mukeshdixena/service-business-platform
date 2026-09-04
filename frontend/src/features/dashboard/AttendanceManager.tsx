import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { Button, Card, EmptyState, ErrorState, FormError, Input, LoadingSpinner } from '../../components/ui'
import { queryKeys } from '../../hooks/queryKeys'
import { useToast } from '../../hooks/useToast'
import { attendanceApi } from '../../services/api/attendanceApi'
import { getFriendlyErrorMessage } from '../../utils/apiError'
import { formatDateTime } from '../../utils/date'

const POLL_INTERVAL_MS = 12000

/**
 * ASSUMPTION (documented per task instructions): API_CONTRACT.md's
 * CreateAttendanceRequest only takes a `customerId` — there is no documented
 * endpoint for a business to search/look up a customer by name or email. This
 * screen therefore asks staff to enter the customer's id directly (e.g. from
 * a membership/booking record, a QR code, or a customer-presented profile
 * screen) rather than inventing a new search API surface.
 */
export function AttendanceManager() {
  const { businessId } = useParams<{ businessId: string }>()
  const [customerId, setCustomerId] = useState('')
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const capacityQuery = useQuery({
    queryKey: queryKeys.businessCapacity(businessId ?? ''),
    queryFn: () => attendanceApi.getCapacity(businessId ?? ''),
    enabled: Boolean(businessId),
    refetchInterval: POLL_INTERVAL_MS,
  })

  const attendanceQuery = useQuery({
    queryKey: queryKeys.businessAttendance(businessId ?? '', { activeOnly: true }),
    queryFn: () => attendanceApi.list(businessId ?? '', { activeOnly: true, size: 100 }),
    enabled: Boolean(businessId),
    refetchInterval: POLL_INTERVAL_MS,
  })

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: queryKeys.businessCapacity(businessId ?? '') })
    void queryClient.invalidateQueries({ queryKey: ['businesses', businessId, 'attendance'] })
  }

  const checkInMutation = useMutation({
    mutationFn: () => attendanceApi.checkIn(businessId ?? '', { customerId: customerId.trim() }),
    onSuccess: () => {
      showToast('Customer checked in.', 'success')
      setCustomerId('')
      invalidate()
    },
  })

  const checkOutMutation = useMutation({
    mutationFn: (attendanceId: string) => attendanceApi.checkOut(businessId ?? '', attendanceId),
    onSuccess: () => {
      showToast('Customer checked out.', 'success')
      invalidate()
    },
    onError: (error: unknown) => showToast(getFriendlyErrorMessage(error), 'error'),
  })

  const openRecords = attendanceQuery.data?.content ?? []

  return (
    <div className="flex flex-col gap-4">
      <h2 className="text-lg font-semibold text-slate-900">Attendance & capacity</h2>

      <Card title="Current occupancy">
        {capacityQuery.isLoading && <LoadingSpinner label="Loading capacity…" />}
        {capacityQuery.isError && <ErrorState error={capacityQuery.error} onRetry={() => capacityQuery.refetch()} />}
        {capacityQuery.isSuccess && (
          <div className="flex items-baseline gap-2">
            <span className="text-3xl font-semibold text-slate-900">{capacityQuery.data.current}</span>
            <span className="text-slate-500">
              {capacityQuery.data.capacity != null ? `/ ${capacityQuery.data.capacity} capacity` : '(no capacity limit set)'}
            </span>
            {capacityQuery.data.available != null && (
              <span className="ml-2 text-sm text-slate-500">{capacityQuery.data.available} spots available</span>
            )}
          </div>
        )}
      </Card>

      <Card title="Check in a customer">
        <form
          onSubmit={(event) => {
            event.preventDefault()
            if (customerId.trim()) checkInMutation.mutate()
          }}
          className="flex flex-col gap-3 sm:flex-row sm:items-end"
        >
          <div className="flex-1">
            <Input
              label="Customer ID"
              hint="Enter the customer's id (see assumption in code comments — no search endpoint exists yet)."
              value={customerId}
              onChange={(event) => setCustomerId(event.target.value)}
              placeholder="e.g. 3f1c2b4a-..."
            />
          </div>
          <Button type="submit" isLoading={checkInMutation.isPending} disabled={!customerId.trim()}>
            Check in
          </Button>
        </form>
        <FormError message={checkInMutation.isError ? getFriendlyErrorMessage(checkInMutation.error) : null} />
      </Card>

      <Card title="Currently checked in">
        {attendanceQuery.isLoading && <LoadingSpinner label="Loading attendance…" />}
        {attendanceQuery.isError && <ErrorState error={attendanceQuery.error} onRetry={() => attendanceQuery.refetch()} />}
        {attendanceQuery.isSuccess && openRecords.length === 0 && (
          <EmptyState title="Nobody checked in" description="Checked-in customers will appear here until they check out." />
        )}
        {attendanceQuery.isSuccess && openRecords.length > 0 && (
          <div className="flex flex-col gap-3">
            {openRecords.map((record) => (
              <div
                key={record.id}
                className="flex flex-col gap-2 rounded-lg border border-slate-200 bg-white p-4 shadow-sm sm:flex-row sm:items-center sm:justify-between"
              >
                <div>
                  <p className="font-medium text-slate-900">Customer #{record.customerId.slice(0, 8)}</p>
                  <p className="text-sm text-slate-500">Checked in {formatDateTime(record.checkInAt)}</p>
                </div>
                <Button
                  variant="secondary"
                  size="sm"
                  isLoading={checkOutMutation.isPending && checkOutMutation.variables === record.id}
                  onClick={() => checkOutMutation.mutate(record.id)}
                >
                  Check out
                </Button>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  )
}

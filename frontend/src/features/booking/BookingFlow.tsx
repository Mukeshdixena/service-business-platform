import { useMutation, useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { SlotPicker } from '../../components/booking/SlotPicker'
import { Button, Card, EmptyState, ErrorState, FormError, LoadingSpinner, Skeleton, TextArea } from '../../components/ui'
import { useToast } from '../../hooks/useToast'
import { queryKeys } from '../../hooks/queryKeys'
import { availabilityApi } from '../../services/api/availabilityApi'
import { customerBookingApi } from '../../services/api/bookingApi'
import { discoveryApi } from '../../services/api/discoveryApi'
import type { AvailabilitySlot } from '../../types'
import { getFriendlyErrorMessage } from '../../utils/apiError'
import { cn } from '../../utils/cn'
import { addDays, formatIsoDateLong, formatTime, todayIsoDate } from '../../utils/date'
import { formatMoney } from '../../utils/money'

const UPCOMING_DAYS = 14

export function BookingFlow() {
  const { slug } = useParams<{ slug: string }>()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const { showToast } = useToast()

  const businessQuery = useQuery({
    queryKey: queryKeys.discoveryBusinessBySlug(slug ?? ''),
    queryFn: () => discoveryApi.getBusinessBySlug(slug ?? ''),
    enabled: Boolean(slug),
  })

  const [serviceId, setServiceId] = useState<string | undefined>(searchParams.get('serviceId') ?? undefined)
  const [staffId, setStaffId] = useState<string | undefined>(undefined)
  const [date, setDate] = useState<string>(todayIsoDate())
  const [selectedSlot, setSelectedSlot] = useState<AvailabilitySlot | null>(null)
  const [notes, setNotes] = useState('')

  const business = businessQuery.data
  const bookableServices = (business?.services ?? []).filter(
    (service) => service.status === 'ACTIVE' && service.bookingType === 'APPOINTMENT',
  )
  const selectedService = bookableServices.find((service) => service.id === serviceId)
  const eligibleStaff = (business?.staff ?? []).filter(
    (member) => member.status === 'ACTIVE' && (!selectedService || member.serviceIds.includes(selectedService.id)),
  )

  // Selecting a different service/staff/date invalidates any previously chosen slot.
  useEffect(() => {
    setSelectedSlot(null)
  }, [serviceId, staffId, date])

  const availabilityQuery = useQuery({
    queryKey: queryKeys.availability(business?.id ?? '', { serviceId: serviceId ?? '', staffId, date }),
    queryFn: () => availabilityApi.get(business?.id ?? '', { serviceId: serviceId ?? '', staffId, date }),
    enabled: Boolean(business?.id && serviceId),
  })

  const createBookingMutation = useMutation({
    mutationFn: () => {
      if (!business || !serviceId || !selectedSlot) {
        throw new Error('Booking selection is incomplete.')
      }
      return customerBookingApi.create(business.id, {
        serviceId,
        staffId: staffId ?? null,
        startAt: selectedSlot.start,
        notes: notes.trim() ? notes.trim() : null,
      })
    },
    onSuccess: () => {
      showToast('Booking requested — the business will confirm shortly.', 'success')
      navigate('/my-bookings')
    },
    onError: () => {
      // The slot may have just been taken by someone else — refresh the list so the user can pick another.
      void availabilityQuery.refetch()
      setSelectedSlot(null)
    },
  })

  if (businessQuery.isLoading) {
    return <LoadingSpinner label="Loading booking options…" className="min-h-[50vh]" />
  }
  if (businessQuery.isError) {
    return <ErrorState error={businessQuery.error} onRetry={() => businessQuery.refetch()} />
  }
  if (!business) return null

  if (!business.capabilities.includes('APPOINTMENTS') || bookableServices.length === 0) {
    return (
      <EmptyState
        title="Online booking isn't available"
        description="This business doesn't currently accept online appointment bookings."
        action={
          <Link to={`/businesses/${business.slug}`}>
            <Button variant="secondary">Back to profile</Button>
          </Link>
        }
      />
    )
  }

  const upcomingDates = Array.from({ length: UPCOMING_DAYS }, (_, index) => addDays(todayIsoDate(), index))

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6">
      <div>
        <Link to={`/businesses/${business.slug}`} className="text-sm text-brand-600 hover:text-brand-700">
          &larr; Back to {business.name}
        </Link>
        <h1 className="mt-1 text-2xl font-semibold text-slate-900">Book an appointment</h1>
      </div>

      <Card title="1. Choose a service">
        <div className="flex flex-col gap-2">
          {bookableServices.map((service) => (
            <label
              key={service.id}
              className={cn(
                'flex cursor-pointer items-center justify-between rounded-md border p-3 text-sm transition-colors',
                serviceId === service.id ? 'border-brand-600 bg-brand-50 ring-1 ring-brand-600' : 'border-slate-200 hover:border-slate-300',
              )}
            >
              <input
                type="radio"
                name="service"
                className="sr-only"
                value={service.id}
                checked={serviceId === service.id}
                onChange={() => {
                  setServiceId(service.id)
                  setStaffId(undefined)
                }}
              />
              <span>
                <span className="block font-medium text-slate-900">{service.name}</span>
                {service.durationMinutes != null && (
                  <span className="block text-xs text-slate-500">{service.durationMinutes} min</span>
                )}
              </span>
              <span className="font-medium text-slate-700">{formatMoney(service.price, service.currency)}</span>
            </label>
          ))}
        </div>
      </Card>

      {serviceId && eligibleStaff.length > 0 && (
        <Card title="2. Choose staff (optional)">
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => setStaffId(undefined)}
              className={cn(
                'rounded-full border px-3 py-1.5 text-sm',
                staffId === undefined ? 'border-brand-600 bg-brand-50 text-brand-700' : 'border-slate-300 text-slate-600',
              )}
            >
              No preference
            </button>
            {eligibleStaff.map((member) => (
              <button
                key={member.id}
                type="button"
                onClick={() => setStaffId(member.id)}
                className={cn(
                  'rounded-full border px-3 py-1.5 text-sm',
                  staffId === member.id ? 'border-brand-600 bg-brand-50 text-brand-700' : 'border-slate-300 text-slate-600',
                )}
              >
                {member.displayName}
              </button>
            ))}
          </div>
        </Card>
      )}

      {serviceId && (
        <Card title="3. Choose a date & time">
          <div className="flex flex-col gap-4">
            <div className="flex gap-2 overflow-x-auto pb-1">
              {upcomingDates.map((day) => (
                <button
                  key={day}
                  type="button"
                  onClick={() => setDate(day)}
                  className={cn(
                    'shrink-0 rounded-md border px-3 py-2 text-xs font-medium',
                    date === day ? 'border-brand-600 bg-brand-600 text-white' : 'border-slate-300 text-slate-600 hover:border-slate-400',
                  )}
                >
                  {formatIsoDateLong(day).split(',')[0]}
                  <span className="block text-[10px] font-normal opacity-80">{day.slice(5)}</span>
                </button>
              ))}
            </div>

            {availabilityQuery.isLoading && (
              <div className="grid grid-cols-4 gap-2">
                {Array.from({ length: 8 }).map((_, index) => (
                  <Skeleton key={index} className="h-9" />
                ))}
              </div>
            )}

            {availabilityQuery.isError && (
              <ErrorState error={availabilityQuery.error} onRetry={() => availabilityQuery.refetch()} />
            )}

            {availabilityQuery.isSuccess &&
              (availabilityQuery.data.status === 'UNAVAILABLE' || availabilityQuery.data.slots.length === 0) && (
                <EmptyState title="No available times" description="Try a different date." />
              )}

            {availabilityQuery.isSuccess && availabilityQuery.data.slots.length > 0 && (
              <SlotPicker slots={availabilityQuery.data.slots} selected={selectedSlot} onSelect={setSelectedSlot} />
            )}
          </div>
        </Card>
      )}

      {selectedSlot && selectedService && (
        <Card title="4. Confirm booking">
          <div className="flex flex-col gap-4">
            <FormError
              message={createBookingMutation.isError ? getFriendlyErrorMessage(createBookingMutation.error) : null}
            />
            <dl className="grid grid-cols-2 gap-2 text-sm">
              <dt className="text-slate-500">Service</dt>
              <dd className="text-right text-slate-900">{selectedService.name}</dd>
              <dt className="text-slate-500">When</dt>
              <dd className="text-right text-slate-900">
                {formatIsoDateLong(date)} at {formatTime(selectedSlot.start)}
              </dd>
              <dt className="text-slate-500">Price</dt>
              <dd className="text-right text-slate-900">{formatMoney(selectedService.price, selectedService.currency)}</dd>
            </dl>
            <TextArea
              label="Notes for the business (optional)"
              value={notes}
              onChange={(event) => setNotes(event.target.value)}
              placeholder="Anything the business should know before your appointment"
            />
            <Button
              onClick={() => createBookingMutation.mutate()}
              isLoading={createBookingMutation.isPending}
              className="w-full sm:w-auto"
            >
              Confirm booking
            </Button>
          </div>
        </Card>
      )}
    </div>
  )
}

import { useMutation, useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { SlotPicker } from '../../components/booking/SlotPicker'
import { Button, Card, EmptyState, ErrorState, FormError, Input, LoadingSpinner, Skeleton, TextArea } from '../../components/ui'
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
import { estimateRentalPrice } from '../../utils/rentalPricing'

const UPCOMING_DAYS = 14

/**
 * A rental booking has two things an appointment booking doesn't: a
 * customer-chosen resource (instead of staff) and a customer-chosen duration
 * (instead of a fixed service duration) — API_CONTRACT.md Phase 7. Rather than
 * threading a second "mode" through BookingFlow.tsx (staff-vs-resource picker,
 * fixed-vs-computed endAt, differing confirm copy) this is a small parallel
 * flow that reuses the same building blocks (SlotPicker, date utils, Card/
 * EmptyState/ErrorState primitives) instead of duplicating the whole
 * component.
 */
export function RentalBookingFlow() {
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
  const [resourceId, setResourceId] = useState<string | undefined>(undefined)
  const [date, setDate] = useState<string>(todayIsoDate())
  const [selectedSlot, setSelectedSlot] = useState<AvailabilitySlot | null>(null)
  const [durationUnits, setDurationUnits] = useState<number>(1)
  const [notes, setNotes] = useState('')

  const business = businessQuery.data
  const rentalServices = (business?.services ?? []).filter(
    (service) => service.status === 'ACTIVE' && service.bookingType === 'RENTAL',
  )
  const selectedService = rentalServices.find((service) => service.id === serviceId)
  const availableResources = (business?.resources ?? []).filter((resource) => resource.status !== 'UNAVAILABLE')

  useEffect(() => {
    setSelectedSlot(null)
  }, [serviceId, resourceId, date])

  const availabilityQuery = useQuery({
    queryKey: queryKeys.availability(business?.id ?? '', { serviceId: serviceId ?? '', staffId: resourceId, date }),
    queryFn: () => availabilityApi.get(business?.id ?? '', { serviceId: serviceId ?? '', resourceId, date }),
    enabled: Boolean(business?.id && serviceId && resourceId),
  })

  const endAt =
    selectedSlot && selectedService?.pricingUnit
      ? new Date(
          new Date(selectedSlot.start).getTime() +
            durationUnits * (selectedService.pricingUnit === 'HOUR' ? 3_600_000 : 86_400_000),
        ).toISOString()
      : null

  const estimatedPrice =
    selectedSlot && endAt && selectedService?.pricingUnit
      ? estimateRentalPrice(selectedService.price, selectedService.pricingUnit, new Date(selectedSlot.start), new Date(endAt))
      : null

  const createBookingMutation = useMutation({
    mutationFn: () => {
      if (!business || !serviceId || !resourceId || !selectedSlot || !endAt) {
        throw new Error('Rental selection is incomplete.')
      }
      return customerBookingApi.create(business.id, {
        serviceId,
        resourceId,
        startAt: selectedSlot.start,
        endAt,
        notes: notes.trim() ? notes.trim() : null,
      })
    },
    onSuccess: () => {
      showToast('Rental booked — the business will confirm shortly.', 'success')
      navigate('/my-bookings')
    },
    onError: () => {
      void availabilityQuery.refetch()
      setSelectedSlot(null)
    },
  })

  if (businessQuery.isLoading) {
    return <LoadingSpinner label="Loading rental options…" className="min-h-[50vh]" />
  }
  if (businessQuery.isError) {
    return <ErrorState error={businessQuery.error} onRetry={() => businessQuery.refetch()} />
  }
  if (!business) return null

  const hasRentalsCapability = business.capabilities.includes('RESOURCES') || business.capabilities.includes('RENTALS')
  if (!hasRentalsCapability || rentalServices.length === 0) {
    return (
      <EmptyState
        title="Rentals aren't available"
        description="This business doesn't currently offer rentable resources."
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
        <h1 className="mt-1 text-2xl font-semibold text-slate-900">Rent a resource</h1>
      </div>

      <Card title="1. Choose what to rent">
        <div className="flex flex-col gap-2">
          {rentalServices.map((service) => (
            <label
              key={service.id}
              className={cn(
                'flex cursor-pointer items-center justify-between rounded-md border p-3 text-sm transition-colors',
                serviceId === service.id ? 'border-brand-600 bg-brand-50 ring-1 ring-brand-600' : 'border-slate-200 hover:border-slate-300',
              )}
            >
              <input
                type="radio"
                name="rentalService"
                className="sr-only"
                value={service.id}
                checked={serviceId === service.id}
                onChange={() => {
                  setServiceId(service.id)
                  setResourceId(undefined)
                }}
              />
              <span className="block font-medium text-slate-900">{service.name}</span>
              <span className="font-medium text-slate-700">
                {formatMoney(service.price, service.currency)} / {service.pricingUnit === 'HOUR' ? 'hour' : 'day'}
              </span>
            </label>
          ))}
        </div>
      </Card>

      {serviceId && (
        <Card title="2. Choose a resource">
          {availableResources.length === 0 ? (
            <EmptyState title="No resources available" description="This business has no resources listed to rent right now." />
          ) : (
            <div className="flex flex-wrap gap-2">
              {availableResources.map((resource) => (
                <button
                  key={resource.id}
                  type="button"
                  onClick={() => setResourceId(resource.id)}
                  className={cn(
                    'rounded-full border px-3 py-1.5 text-sm',
                    resourceId === resource.id ? 'border-brand-600 bg-brand-50 text-brand-700' : 'border-slate-300 text-slate-600',
                  )}
                >
                  {resource.name} ({resource.identifier})
                </button>
              ))}
            </div>
          )}
        </Card>
      )}

      {serviceId && resourceId && (
        <Card title="3. Choose a start date & time">
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
                <EmptyState title="No available start times" description="Try a different date." />
              )}

            {availabilityQuery.isSuccess && availabilityQuery.data.slots.length > 0 && (
              <SlotPicker slots={availabilityQuery.data.slots} selected={selectedSlot} onSelect={setSelectedSlot} />
            )}

            {selectedSlot && selectedService && (
              <Input
                label={`Duration (${selectedService.pricingUnit === 'HOUR' ? 'hours' : 'days'})`}
                type="number"
                min="1"
                value={durationUnits}
                onChange={(event) => setDurationUnits(Math.max(1, Number(event.target.value) || 1))}
              />
            )}
          </div>
        </Card>
      )}

      {selectedSlot && selectedService && endAt && (
        <Card title="4. Confirm rental">
          <div className="flex flex-col gap-4">
            <FormError
              message={createBookingMutation.isError ? getFriendlyErrorMessage(createBookingMutation.error) : null}
            />
            <dl className="grid grid-cols-2 gap-2 text-sm">
              <dt className="text-slate-500">Resource</dt>
              <dd className="text-right text-slate-900">
                {availableResources.find((resource) => resource.id === resourceId)?.name}
              </dd>
              <dt className="text-slate-500">From</dt>
              <dd className="text-right text-slate-900">
                {formatIsoDateLong(date)} at {formatTime(selectedSlot.start)}
              </dd>
              <dt className="text-slate-500">Until</dt>
              <dd className="text-right text-slate-900">{formatTime(endAt)}</dd>
              <dt className="text-slate-500">Estimated price</dt>
              <dd className="text-right text-slate-900">
                {estimatedPrice != null ? formatMoney(estimatedPrice, selectedService.currency) : '—'}
              </dd>
            </dl>
            <p className="text-xs text-slate-500">
              The final price is calculated by the business at confirmation and may differ slightly from this estimate.
            </p>
            <TextArea
              label="Notes for the business (optional)"
              value={notes}
              onChange={(event) => setNotes(event.target.value)}
              placeholder="Anything the business should know before your rental"
            />
            <Button
              onClick={() => createBookingMutation.mutate()}
              isLoading={createBookingMutation.isPending}
              className="w-full sm:w-auto"
            >
              Confirm rental
            </Button>
          </div>
        </Card>
      )}
    </div>
  )
}

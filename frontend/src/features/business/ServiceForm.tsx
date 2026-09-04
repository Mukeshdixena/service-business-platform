import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Button, FormError, Input, Select, TextArea } from '../../components/ui'
import { SERVICE_BOOKING_TYPES } from '../../types'
import type { ServiceDto } from '../../types'
import { serviceFormSchema, type ServiceFormValues } from './schemas'

const BOOKING_TYPE_LABELS: Record<(typeof SERVICE_BOOKING_TYPES)[number], string> = {
  APPOINTMENT: 'Appointment (bookable online)',
  QUEUE: 'Queue (walk-in, later phase)',
  REQUEST: 'Request (later phase)',
  RENTAL: 'Rental (later phase)',
  WALK_IN: 'Walk-in (later phase)',
}
const BOOKING_TYPE_OPTIONS = SERVICE_BOOKING_TYPES.map((type) => ({ value: type, label: BOOKING_TYPE_LABELS[type] }))

export interface ServiceFormProps {
  initialValue?: ServiceDto
  onSubmit: (values: ServiceFormValues) => void
  isSubmitting: boolean
  submitError?: string | null
  submitLabel?: string
}

export function ServiceForm({ initialValue, onSubmit, isSubmitting, submitError, submitLabel = 'Save' }: ServiceFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ServiceFormValues>({
    resolver: zodResolver(serviceFormSchema),
    defaultValues: {
      name: initialValue?.name ?? '',
      description: initialValue?.description ?? '',
      price: initialValue?.price ?? 0,
      currency: initialValue?.currency ?? 'INR',
      durationMinutes: initialValue?.durationMinutes ?? 30,
      bookingType: initialValue?.bookingType ?? 'APPOINTMENT',
    },
  })

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
      <FormError message={submitError} />
      <Input label="Service name" error={errors.name?.message} {...register('name')} />
      <TextArea label="Description" error={errors.description?.message} {...register('description')} />
      <div className="grid gap-4 sm:grid-cols-2">
        <Input label="Price" type="number" step="0.01" min="0" error={errors.price?.message} {...register('price')} />
        <Input label="Currency" placeholder="INR" error={errors.currency?.message} {...register('currency')} />
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <Input
          label="Duration (minutes)"
          type="number"
          min="1"
          error={errors.durationMinutes?.message}
          {...register('durationMinutes')}
        />
        <Select
          label="Booking type"
          options={BOOKING_TYPE_OPTIONS}
          error={errors.bookingType?.message}
          {...register('bookingType')}
        />
      </div>
      <Button type="submit" isLoading={isSubmitting} className="w-full sm:w-auto">
        {submitLabel}
      </Button>
    </form>
  )
}

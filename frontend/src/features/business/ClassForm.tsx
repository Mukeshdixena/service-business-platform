import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Button, FormError, Input, Select, TextArea } from '../../components/ui'
import type { ClassDto, StaffDto } from '../../types'
import { classFormSchema, type ClassFormValues } from './schemas'

/** "2026-09-10T09:00:00Z" -> "2026-09-10T09:00" (local) for a datetime-local input. */
function toLocalInputValue(iso: string): string {
  const date = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** Reverse of toLocalInputValue — converts a datetime-local value back to an ISO-8601 UTC instant. */
export function localInputValueToIso(value: string): string {
  return new Date(value).toISOString()
}

export interface ClassFormProps {
  initialValue?: ClassDto
  staffOptions: StaffDto[]
  onSubmit: (values: ClassFormValues) => void
  isSubmitting: boolean
  submitError?: string | null
  submitLabel?: string
}

export function ClassForm({
  initialValue,
  staffOptions,
  onSubmit,
  isSubmitting,
  submitError,
  submitLabel = 'Save',
}: ClassFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ClassFormValues>({
    resolver: zodResolver(classFormSchema),
    defaultValues: {
      name: initialValue?.name ?? '',
      description: initialValue?.description ?? '',
      staffId: initialValue?.staffId ?? '',
      startAt: initialValue ? toLocalInputValue(initialValue.startAt) : '',
      endAt: initialValue ? toLocalInputValue(initialValue.endAt) : '',
      capacity: initialValue?.capacity ?? 10,
    },
  })

  const staffSelectOptions = [
    { value: '', label: 'No instructor assigned' },
    ...staffOptions.map((staff) => ({ value: staff.id, label: staff.displayName })),
  ]

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
      <FormError message={submitError} />
      <Input label="Class name" error={errors.name?.message} {...register('name')} />
      <TextArea label="Description" error={errors.description?.message} {...register('description')} />
      <Select label="Instructor" options={staffSelectOptions} error={errors.staffId?.message} {...register('staffId')} />
      <div className="grid gap-4 sm:grid-cols-2">
        <Input label="Starts at" type="datetime-local" error={errors.startAt?.message} {...register('startAt')} />
        <Input label="Ends at" type="datetime-local" error={errors.endAt?.message} {...register('endAt')} />
      </div>
      <Input label="Capacity" type="number" min="1" error={errors.capacity?.message} {...register('capacity')} />
      <Button type="submit" isLoading={isSubmitting} className="w-full sm:w-auto">
        {submitLabel}
      </Button>
    </form>
  )
}

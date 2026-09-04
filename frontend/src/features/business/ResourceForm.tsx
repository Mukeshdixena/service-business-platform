import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Button, FormError, Input, Select, TextArea } from '../../components/ui'
import { RESOURCE_STATUSES } from '../../types'
import type { ResourceDto } from '../../types'
import { resourceFormSchema, type ResourceFormValues } from './schemas'

const STATUS_OPTIONS = RESOURCE_STATUSES.map((status) => ({
  value: status,
  label: status.charAt(0) + status.slice(1).toLowerCase().replace('_', ' '),
}))

export interface ResourceFormProps {
  initialValue?: ResourceDto
  onSubmit: (values: ResourceFormValues) => void
  isSubmitting: boolean
  submitError?: string | null
  submitLabel?: string
}

export function ResourceForm({ initialValue, onSubmit, isSubmitting, submitError, submitLabel = 'Save' }: ResourceFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ResourceFormValues>({
    resolver: zodResolver(resourceFormSchema),
    defaultValues: {
      name: initialValue?.name ?? '',
      type: initialValue?.type ?? '',
      description: initialValue?.description ?? '',
      imageUrl: initialValue?.imageUrl ?? '',
      identifier: initialValue?.identifier ?? '',
      status: initialValue?.status ?? 'AVAILABLE',
    },
  })

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
      <FormError message={submitError} />
      <Input label="Name" error={errors.name?.message} {...register('name')} />
      <div className="grid gap-4 sm:grid-cols-2">
        <Input label="Type" placeholder="e.g. EXCAVATOR" error={errors.type?.message} {...register('type')} />
        <Input label="Identifier" placeholder="e.g. JCB-01" error={errors.identifier?.message} {...register('identifier')} />
      </div>
      <TextArea label="Description" error={errors.description?.message} {...register('description')} />
      <div className="grid gap-4 sm:grid-cols-2">
        <Input label="Image URL" error={errors.imageUrl?.message} {...register('imageUrl')} />
        <Select label="Status" options={STATUS_OPTIONS} error={errors.status?.message} {...register('status')} />
      </div>
      <Button type="submit" isLoading={isSubmitting} className="w-full sm:w-auto">
        {submitLabel}
      </Button>
    </form>
  )
}

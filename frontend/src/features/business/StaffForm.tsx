import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Button, FormError, Input, TextArea } from '../../components/ui'
import type { ServiceDto, StaffDto } from '../../types'
import { cn } from '../../utils/cn'
import { staffFormSchema, type StaffFormValues } from './schemas'

export interface StaffFormProps {
  initialValue?: StaffDto
  availableServices: ServiceDto[]
  onSubmit: (values: StaffFormValues) => void
  isSubmitting: boolean
  submitError?: string | null
  submitLabel?: string
}

export function StaffForm({
  initialValue,
  availableServices,
  onSubmit,
  isSubmitting,
  submitError,
  submitLabel = 'Save',
}: StaffFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<StaffFormValues>({
    resolver: zodResolver(staffFormSchema),
    defaultValues: {
      displayName: initialValue?.displayName ?? '',
      title: initialValue?.title ?? '',
      bio: initialValue?.bio ?? '',
      imageUrl: initialValue?.imageUrl ?? '',
      serviceIds: initialValue?.serviceIds ?? [],
    },
  })

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
      <FormError message={submitError} />
      <Input label="Display name" error={errors.displayName?.message} {...register('displayName')} />
      <Input label="Title" placeholder="e.g. Senior Stylist" error={errors.title?.message} {...register('title')} />
      <TextArea label="Bio" error={errors.bio?.message} {...register('bio')} />
      <Input label="Image URL" error={errors.imageUrl?.message} {...register('imageUrl')} />

      <div>
        <span className="text-sm font-medium text-slate-700">Assigned services</span>
        {availableServices.length === 0 ? (
          <p className="mt-1 text-sm text-slate-500">Add a service first to assign it to staff.</p>
        ) : (
          <div className="mt-1 flex flex-col gap-2">
            {availableServices.map((service) => (
              <label
                key={service.id}
                className={cn('flex items-center gap-2 rounded-md border border-slate-200 px-3 py-2 text-sm')}
              >
                <input type="checkbox" value={service.id} {...register('serviceIds')} className="h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500" />
                {service.name}
              </label>
            ))}
          </div>
        )}
      </div>

      <Button type="submit" isLoading={isSubmitting} className="w-full sm:w-auto">
        {submitLabel}
      </Button>
    </form>
  )
}

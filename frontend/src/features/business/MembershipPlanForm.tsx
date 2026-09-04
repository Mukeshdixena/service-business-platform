import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Button, FormError, Input, Select, TextArea } from '../../components/ui'
import { MEMBERSHIP_DURATION_UNITS } from '../../types'
import type { MembershipPlanDto } from '../../types'
import { membershipPlanFormSchema, type MembershipPlanFormValues } from './schemas'

const DURATION_UNIT_OPTIONS = MEMBERSHIP_DURATION_UNITS.map((unit) => ({
  value: unit,
  label: unit.charAt(0) + unit.slice(1).toLowerCase(),
}))

export interface MembershipPlanFormProps {
  initialValue?: MembershipPlanDto
  onSubmit: (values: MembershipPlanFormValues) => void
  isSubmitting: boolean
  submitError?: string | null
  submitLabel?: string
}

export function MembershipPlanForm({
  initialValue,
  onSubmit,
  isSubmitting,
  submitError,
  submitLabel = 'Save',
}: MembershipPlanFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<MembershipPlanFormValues>({
    resolver: zodResolver(membershipPlanFormSchema),
    defaultValues: {
      name: initialValue?.name ?? '',
      description: initialValue?.description ?? '',
      price: initialValue?.price ?? 0,
      currency: initialValue?.currency ?? 'INR',
      duration: initialValue?.duration ?? 1,
      durationUnit: initialValue?.durationUnit ?? 'MONTH',
    },
  })

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
      <FormError message={submitError} />
      <Input label="Plan name" error={errors.name?.message} {...register('name')} />
      <TextArea label="Description" error={errors.description?.message} {...register('description')} />
      <div className="grid gap-4 sm:grid-cols-2">
        <Input label="Price" type="number" step="0.01" min="0" error={errors.price?.message} {...register('price')} />
        <Input label="Currency" placeholder="INR" error={errors.currency?.message} {...register('currency')} />
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <Input label="Duration" type="number" min="1" error={errors.duration?.message} {...register('duration')} />
        <Select
          label="Duration unit"
          options={DURATION_UNIT_OPTIONS}
          error={errors.durationUnit?.message}
          {...register('durationUnit')}
        />
      </div>
      <Button type="submit" isLoading={isSubmitting} className="w-full sm:w-auto">
        {submitLabel}
      </Button>
    </form>
  )
}

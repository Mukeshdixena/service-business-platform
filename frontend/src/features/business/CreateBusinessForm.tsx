import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { Button, FormError, Input, Select, TextArea } from '../../components/ui'
import { useAuth } from '../auth/AuthContext'
import { businessApi } from '../../services/api/businessApi'
import { BUSINESS_CATEGORIES } from '../../types'
import { CATEGORY_LABELS } from '../../utils/categoryLabels'
import { getFriendlyErrorMessage } from '../../utils/apiError'
import { createBusinessSchema, type CreateBusinessFormValues } from './schemas'

const CATEGORY_OPTIONS = BUSINESS_CATEGORIES.map((category) => ({ value: category, label: CATEGORY_LABELS[category] }))

export function CreateBusinessForm() {
  const { refreshMemberships } = useAuth()
  const navigate = useNavigate()

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CreateBusinessFormValues>({
    resolver: zodResolver(createBusinessSchema),
    defaultValues: { name: '', description: '', phone: '', email: '' },
  })

  const createMutation = useMutation({
    mutationFn: (values: CreateBusinessFormValues) =>
      businessApi.create({
        name: values.name,
        category: values.category,
        description: values.description || null,
        phone: values.phone || null,
        email: values.email || null,
      }),
    onSuccess: async (business) => {
      await refreshMemberships()
      navigate(`/dashboard/${business.id}/profile`, { replace: true })
    },
  })

  return (
    <form onSubmit={handleSubmit((values) => createMutation.mutate(values))} noValidate className="flex flex-col gap-4">
      <FormError message={createMutation.isError ? getFriendlyErrorMessage(createMutation.error) : null} />
      <Input label="Business name" error={errors.name?.message} {...register('name')} />
      <Select
        label="Category"
        placeholder="Select a category"
        options={CATEGORY_OPTIONS}
        error={errors.category?.message}
        {...register('category')}
      />
      <TextArea label="Description (optional)" error={errors.description?.message} {...register('description')} />
      <Input label="Phone (optional)" error={errors.phone?.message} {...register('phone')} />
      <Input label="Email (optional)" type="email" error={errors.email?.message} {...register('email')} />
      <Button type="submit" isLoading={createMutation.isPending} className="w-full sm:w-auto">
        Create business
      </Button>
    </form>
  )
}

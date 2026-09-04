import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { useParams } from 'react-router-dom'
import { BusinessStatusBadge } from '../../components/business/BusinessStatusBadge'
import { Badge, Button, Card, ErrorState, FormError, Input, LoadingSpinner, TextArea } from '../../components/ui'
import { queryKeys } from '../../hooks/queryKeys'
import { useToast } from '../../hooks/useToast'
import { businessApi } from '../../services/api/businessApi'
import { getFriendlyErrorMessage } from '../../utils/apiError'
import { CATEGORY_LABELS } from '../../utils/categoryLabels'
import { businessProfileSchema, type BusinessProfileFormValues } from './schemas'

export function BusinessProfileEditor() {
  const { businessId } = useParams<{ businessId: string }>()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const businessQuery = useQuery({
    queryKey: queryKeys.business(businessId ?? ''),
    queryFn: () => businessApi.get(businessId ?? ''),
    enabled: Boolean(businessId),
  })

  const {
    register,
    handleSubmit,
    formState: { errors, isDirty },
  } = useForm<BusinessProfileFormValues>({
    resolver: zodResolver(businessProfileSchema),
    values: businessQuery.data
      ? {
          name: businessQuery.data.name,
          description: businessQuery.data.description ?? '',
          phone: businessQuery.data.phone ?? '',
          email: businessQuery.data.email ?? '',
          logoUrl: businessQuery.data.logoUrl ?? '',
          coverImageUrl: businessQuery.data.coverImageUrl ?? '',
          maxCapacity: businessQuery.data.maxCapacity ?? undefined,
        }
      : undefined,
  })

  const updateMutation = useMutation({
    mutationFn: (values: BusinessProfileFormValues) =>
      businessApi.update(businessId ?? '', {
        name: values.name,
        description: values.description || null,
        phone: values.phone || null,
        email: values.email || null,
        logoUrl: values.logoUrl || null,
        coverImageUrl: values.coverImageUrl || null,
        maxCapacity: values.maxCapacity ?? null,
      }),
    onSuccess: (business) => {
      queryClient.setQueryData(queryKeys.business(businessId ?? ''), business)
      showToast('Profile updated.', 'success')
    },
  })

  const publishMutation = useMutation({
    mutationFn: () => businessApi.publish(businessId ?? ''),
    onSuccess: (business) => {
      queryClient.setQueryData(queryKeys.business(businessId ?? ''), business)
      showToast('Business published — it is now visible to customers.', 'success')
    },
  })

  if (businessQuery.isLoading) return <LoadingSpinner label="Loading business profile…" />
  if (businessQuery.isError) return <ErrorState error={businessQuery.error} onRetry={() => businessQuery.refetch()} />
  const business = businessQuery.data
  if (!business) return null

  return (
    <div className="flex flex-col gap-4">
      <Card
        title="Status"
        actions={
          business.status === 'DRAFT' ? (
            <Button size="sm" isLoading={publishMutation.isPending} onClick={() => publishMutation.mutate()}>
              Publish business
            </Button>
          ) : undefined
        }
      >
        <FormError message={publishMutation.isError ? getFriendlyErrorMessage(publishMutation.error) : null} />
        <div className="flex flex-wrap items-center gap-2">
          <BusinessStatusBadge status={business.status} />
          <Badge color="blue">{CATEGORY_LABELS[business.category]}</Badge>
          <Badge color="slate">{business.verificationStatus}</Badge>
        </div>
        {business.status === 'DRAFT' && (
          <p className="mt-2 text-sm text-slate-500">
            Your business is in draft and not visible to customers yet. Add services and hours, then publish when ready.
          </p>
        )}
      </Card>

      <Card title="Business profile">
        <form
          onSubmit={handleSubmit((values) => updateMutation.mutate(values))}
          noValidate
          className="flex flex-col gap-4"
        >
          <FormError message={updateMutation.isError ? getFriendlyErrorMessage(updateMutation.error) : null} />
          <Input label="Business name" error={errors.name?.message} {...register('name')} />
          <TextArea label="Description" error={errors.description?.message} {...register('description')} />
          <div className="grid gap-4 sm:grid-cols-2">
            <Input label="Phone" error={errors.phone?.message} {...register('phone')} />
            <Input label="Email" type="email" error={errors.email?.message} {...register('email')} />
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <Input label="Logo URL" error={errors.logoUrl?.message} {...register('logoUrl')} />
            <Input label="Cover image URL" error={errors.coverImageUrl?.message} {...register('coverImageUrl')} />
          </div>
          {business.capabilities.includes('CAPACITY') && (
            <Input
              label="Max capacity"
              type="number"
              min="1"
              hint="Maximum number of people allowed on-site at once."
              error={errors.maxCapacity?.message}
              {...register('maxCapacity')}
            />
          )}
          <Button type="submit" isLoading={updateMutation.isPending} disabled={!isDirty} className="w-full sm:w-auto">
            Save changes
          </Button>
        </form>
      </Card>
    </div>
  )
}

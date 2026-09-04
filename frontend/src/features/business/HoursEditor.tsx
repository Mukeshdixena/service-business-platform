import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useFieldArray, useForm } from 'react-hook-form'
import { useParams } from 'react-router-dom'
import { Button, Card, ErrorState, FormError, LoadingSpinner } from '../../components/ui'
import { queryKeys } from '../../hooks/queryKeys'
import { useToast } from '../../hooks/useToast'
import { businessApi } from '../../services/api/businessApi'
import { DAYS_OF_WEEK } from '../../types'
import type { DayOfWeek } from '../../types'
import { getFriendlyErrorMessage } from '../../utils/apiError'
import { hoursFormSchema, type HoursFormValues } from './schemas'

const DAY_LABELS: Record<DayOfWeek, string> = {
  MONDAY: 'Monday',
  TUESDAY: 'Tuesday',
  WEDNESDAY: 'Wednesday',
  THURSDAY: 'Thursday',
  FRIDAY: 'Friday',
  SATURDAY: 'Saturday',
  SUNDAY: 'Sunday',
}

export function HoursEditor() {
  const { businessId } = useParams<{ businessId: string }>()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const hoursQuery = useQuery({
    queryKey: queryKeys.businessHours(businessId ?? ''),
    queryFn: () => businessApi.getHours(businessId ?? ''),
    enabled: Boolean(businessId),
  })

  const {
    control,
    register,
    handleSubmit,
    formState: { errors, isDirty },
  } = useForm<HoursFormValues>({
    resolver: zodResolver(hoursFormSchema),
    values: hoursQuery.data
      ? { hours: hoursQuery.data.map((row) => ({ dayOfWeek: row.dayOfWeek, openTime: row.openTime, closeTime: row.closeTime })) }
      : undefined,
  })

  const { fields, append, remove } = useFieldArray({ control, name: 'hours' })

  const saveMutation = useMutation({
    mutationFn: (values: HoursFormValues) => businessApi.replaceHours(businessId ?? '', { hours: values.hours }),
    onSuccess: (hours) => {
      queryClient.setQueryData(queryKeys.businessHours(businessId ?? ''), hours)
      showToast('Business hours updated.', 'success')
    },
  })

  if (hoursQuery.isLoading) return <LoadingSpinner label="Loading business hours…" />
  if (hoursQuery.isError) return <ErrorState error={hoursQuery.error} onRetry={() => hoursQuery.refetch()} />

  return (
    <Card title="Business hours">
      <form onSubmit={handleSubmit((values) => saveMutation.mutate(values))} noValidate className="flex flex-col gap-6">
        <FormError message={saveMutation.isError ? getFriendlyErrorMessage(saveMutation.error) : null} />
        {DAYS_OF_WEEK.map((day) => {
          const rowsForDay = fields
            .map((field, index) => ({ field, index }))
            .filter(({ field }) => field.dayOfWeek === day)

          return (
            <div key={day} className="flex flex-col gap-2 border-b border-slate-100 pb-4 last:border-b-0 last:pb-0">
              <div className="flex items-center justify-between">
                <span className="text-sm font-semibold text-slate-700">{DAY_LABELS[day]}</span>
                <Button type="button" variant="ghost" size="sm" onClick={() => append({ dayOfWeek: day, openTime: '09:00', closeTime: '17:00' })}>
                  + Add interval
                </Button>
              </div>
              {rowsForDay.length === 0 && <p className="text-sm text-slate-400">Closed</p>}
              {rowsForDay.map(({ index }) => (
                <div key={fields[index]?.id ?? index} className="flex flex-wrap items-center gap-2">
                  <input
                    type="time"
                    className="rounded-md border border-slate-300 px-2 py-1.5 text-sm"
                    {...register(`hours.${index}.openTime`)}
                  />
                  <span className="text-slate-400">to</span>
                  <input
                    type="time"
                    className="rounded-md border border-slate-300 px-2 py-1.5 text-sm"
                    {...register(`hours.${index}.closeTime`)}
                  />
                  <button
                    type="button"
                    onClick={() => remove(index)}
                    className="text-sm text-red-600 hover:text-red-700"
                    aria-label="Remove interval"
                  >
                    Remove
                  </button>
                  {errors.hours?.[index]?.closeTime && (
                    <p className="w-full text-sm text-red-600">{errors.hours[index]?.closeTime?.message}</p>
                  )}
                </div>
              ))}
            </div>
          )
        })}
        <Button type="submit" isLoading={saveMutation.isPending} disabled={!isDirty} className="w-full sm:w-auto">
          Save hours
        </Button>
      </form>
    </Card>
  )
}

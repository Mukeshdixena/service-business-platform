import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { ReviewCard } from '../../components/review/ReviewCard'
import { Button, EmptyState, ErrorState, LoadingSpinner, Pagination, Select } from '../../components/ui'
import { useToast } from '../../hooks/useToast'
import { businessReviewApi } from '../../services/api/reviewApi'
import { REVIEW_STATUSES } from '../../types'
import type { ReviewStatus } from '../../types'

const PAGE_SIZE = 20

const STATUS_OPTIONS = [
  { value: '', label: 'All statuses' },
  ...REVIEW_STATUSES.map((status) => ({ value: status, label: status.replace('_', ' ') })),
]

export function ReviewsManager() {
  const { businessId } = useParams<{ businessId: string }>()
  const [statusFilter, setStatusFilter] = useState<ReviewStatus | ''>('')
  const [page, setPage] = useState(0)
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const params = { status: statusFilter || undefined, page, size: PAGE_SIZE }

  const reviewsQuery = useQuery({
    queryKey: ['businesses', businessId, 'reviews', params],
    queryFn: () => businessReviewApi.list(businessId ?? '', params),
    enabled: Boolean(businessId),
  })

  const approveMutation = useMutation({
    mutationFn: (reviewId: string) => businessReviewApi.approve(businessId ?? '', reviewId),
    onSuccess: () => {
      showToast('Review approved.', 'success')
      void queryClient.invalidateQueries({ queryKey: ['businesses', businessId, 'reviews'] })
    },
  })

  const rejectMutation = useMutation({
    mutationFn: (reviewId: string) => businessReviewApi.reject(businessId ?? '', reviewId),
    onSuccess: () => {
      showToast('Review rejected.', 'success')
      void queryClient.invalidateQueries({ queryKey: ['businesses', businessId, 'reviews'] })
    },
  })

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h2 className="text-lg font-semibold text-slate-900">Reviews</h2>
        <div className="w-48">
          <Select
            options={STATUS_OPTIONS}
            value={statusFilter}
            onChange={(event) => {
              setStatusFilter(event.target.value as ReviewStatus | '')
              setPage(0)
            }}
          />
        </div>
      </div>

      {reviewsQuery.isLoading && <LoadingSpinner label="Loading reviews…" />}
      {reviewsQuery.isError && <ErrorState error={reviewsQuery.error} onRetry={() => reviewsQuery.refetch()} />}
      {reviewsQuery.isSuccess && reviewsQuery.data.content.length === 0 && (
        <EmptyState title="No reviews" description="Customer reviews will appear here." />
      )}

      {reviewsQuery.isSuccess && reviewsQuery.data.content.length > 0 && (
        <>
          <div className="flex flex-col gap-3">
            {reviewsQuery.data.content.map((review) => (
              <ReviewCard
                key={review.id}
                review={review}
                actions={
                  review.status === 'PENDING' ? (
                    <div className="flex gap-2">
                      <Button
                        size="sm"
                        variant="primary"
                        isLoading={approveMutation.isPending && approveMutation.variables === review.id}
                        onClick={() => approveMutation.mutate(review.id)}
                      >
                        Approve
                      </Button>
                      <Button
                        size="sm"
                        variant="danger"
                        isLoading={rejectMutation.isPending && rejectMutation.variables === review.id}
                        onClick={() => rejectMutation.mutate(review.id)}
                      >
                        Reject
                      </Button>
                    </div>
                  ) : undefined
                }
              />
            ))}
          </div>
          <Pagination page={reviewsQuery.data.page} totalPages={reviewsQuery.data.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  )
}

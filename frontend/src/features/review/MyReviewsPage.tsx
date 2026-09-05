import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { ReviewCard } from '../../components/review/ReviewCard'
import { EmptyState, ErrorState, LoadingSpinner, Pagination, Select } from '../../components/ui'
import { useEntityLookup } from '../../hooks/useEntityLookup'
import { customerReviewApi } from '../../services/api/reviewApi'
import { REVIEW_STATUSES } from '../../types'
import type { ReviewStatus } from '../../types'

const PAGE_SIZE = 10

const STATUS_OPTIONS = [
  { value: '', label: 'All statuses' },
  ...REVIEW_STATUSES.map((status) => ({ value: status, label: status.replace('_', ' ') })),
]

export function MyReviewsPage() {
  const [statusFilter, setStatusFilter] = useState<ReviewStatus | ''>('')
  const [page, setPage] = useState(0)
  const lookup = useEntityLookup()

  const params = { status: statusFilter || undefined, page, size: PAGE_SIZE }

  const reviewsQuery = useQuery({
    queryKey: ['me', 'reviews', params],
    queryFn: () => customerReviewApi.listMine(params),
  })

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">My reviews</h1>
          <p className="mt-1 text-sm text-slate-500">Reviews you&apos;ve left for businesses.</p>
        </div>
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

      {reviewsQuery.isLoading && <LoadingSpinner label="Loading your reviews…" />}
      {reviewsQuery.isError && <ErrorState error={reviewsQuery.error} onRetry={() => reviewsQuery.refetch()} />}
      {reviewsQuery.isSuccess && reviewsQuery.data.content.length === 0 && (
        <EmptyState title="No reviews yet" description="When you leave a review, it will show up here." />
      )}

      {reviewsQuery.isSuccess && reviewsQuery.data.content.length > 0 && (
        <>
          <div className="flex flex-col gap-3">
            {reviewsQuery.data.content.map((review) => (
              <ReviewCard
                key={review.id}
                review={review}
                businessName={lookup(review.businessId, '', '').businessName}
              />
            ))}
          </div>
          <Pagination page={reviewsQuery.data.page} totalPages={reviewsQuery.data.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  )
}

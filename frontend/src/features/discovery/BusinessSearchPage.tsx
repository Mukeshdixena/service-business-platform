import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { BusinessCard } from '../../components/business/BusinessCard'
import { EmptyState, ErrorState, Input, Pagination, Select, Skeleton } from '../../components/ui'
import { discoveryApi } from '../../services/api/discoveryApi'
import { queryKeys } from '../../hooks/queryKeys'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'
import type { BusinessCategory } from '../../types'

const PAGE_SIZE = 12

export function BusinessSearchPage() {
  const [query, setQuery] = useState('')
  const [city, setCity] = useState('')
  const [category, setCategory] = useState<BusinessCategory | ''>('')
  const [page, setPage] = useState(0)

  const debouncedQuery = useDebouncedValue(query)
  const debouncedCity = useDebouncedValue(city)

  useEffect(() => {
    setPage(0)
  }, [debouncedQuery, debouncedCity, category])

  const categoriesQuery = useQuery({
    queryKey: queryKeys.discoveryCategories,
    queryFn: discoveryApi.listCategories,
  })

  const searchParams = {
    query: debouncedQuery || undefined,
    city: debouncedCity || undefined,
    category: category || undefined,
    page,
    size: PAGE_SIZE,
  }

  const businessesQuery = useQuery({
    queryKey: queryKeys.discoveryBusinesses(searchParams),
    queryFn: () => discoveryApi.searchBusinesses(searchParams),
    placeholderData: keepPreviousData,
  })

  const categoryOptions = [
    { value: '', label: 'All categories' },
    ...(categoriesQuery.data ?? []).map((option) => ({ value: option.value, label: option.label })),
  ]

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Find a business</h1>
        <p className="mt-1 text-sm text-slate-500">Search salons, clinics, gyms, and rental businesses near you.</p>
      </div>

      <div className="grid gap-3 rounded-lg border border-slate-200 bg-white p-4 shadow-sm sm:grid-cols-3">
        <Input
          label="Search"
          placeholder="Business or service name"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />
        <Select
          label="Category"
          options={categoryOptions}
          value={category}
          onChange={(event) => setCategory(event.target.value as BusinessCategory | '')}
        />
        <Input label="City" placeholder="e.g. Bengaluru" value={city} onChange={(event) => setCity(event.target.value)} />
      </div>

      {businessesQuery.isLoading && (
        <div className="grid gap-4 sm:grid-cols-2">
          {Array.from({ length: 6 }).map((_, index) => (
            <Skeleton key={index} className="h-24" />
          ))}
        </div>
      )}

      {businessesQuery.isError && <ErrorState error={businessesQuery.error} onRetry={() => businessesQuery.refetch()} />}

      {businessesQuery.isSuccess && businessesQuery.data.content.length === 0 && (
        <EmptyState
          title="No businesses found"
          description="Try a different search term, category, or city."
        />
      )}

      {businessesQuery.isSuccess && businessesQuery.data.content.length > 0 && (
        <>
          <div className="grid gap-4 sm:grid-cols-2">
            {businessesQuery.data.content.map((business) => (
              <BusinessCard key={business.id} business={business} />
            ))}
          </div>
          <Pagination page={businessesQuery.data.page} totalPages={businessesQuery.data.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  )
}

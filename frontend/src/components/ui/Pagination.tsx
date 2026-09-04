import { Button } from './Button'

export interface PaginationProps {
  /** 0-based current page, matching the backend's `page` param. */
  page: number
  totalPages: number
  onPageChange: (page: number) => void
}

export function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null

  return (
    <div className="flex items-center justify-between border-t border-slate-100 pt-4">
      <p className="text-sm text-slate-500">
        Page {page + 1} of {totalPages}
      </p>
      <div className="flex gap-2">
        <Button
          variant="secondary"
          size="sm"
          onClick={() => onPageChange(page - 1)}
          disabled={page <= 0}
        >
          Previous
        </Button>
        <Button
          variant="secondary"
          size="sm"
          onClick={() => onPageChange(page + 1)}
          disabled={page + 1 >= totalPages}
        >
          Next
        </Button>
      </div>
    </div>
  )
}

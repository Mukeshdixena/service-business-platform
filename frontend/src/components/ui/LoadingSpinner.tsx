import { cn } from '../../utils/cn'

export interface LoadingSpinnerProps {
  label?: string
  className?: string
}

/** Distinct, real loading state — used wherever a query is in flight. */
export function LoadingSpinner({ label = 'Loading…', className }: LoadingSpinnerProps) {
  return (
    <div className={cn('flex flex-col items-center justify-center gap-3 py-12 text-slate-500', className)}>
      <svg className="h-8 w-8 animate-spin text-brand-600" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
      </svg>
      <p className="text-sm" role="status">
        {label}
      </p>
    </div>
  )
}

export interface SkeletonProps {
  className?: string
}

/** Pulsing placeholder block, for skeleton-style loading states on lists/cards. */
export function Skeleton({ className }: SkeletonProps) {
  return <div className={cn('animate-pulse rounded-md bg-slate-200', className)} aria-hidden="true" />
}

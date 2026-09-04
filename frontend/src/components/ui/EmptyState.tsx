import type { ReactNode } from 'react'

export interface EmptyStateProps {
  title: string
  description?: string
  action?: ReactNode
  icon?: ReactNode
}

/** Distinct, real empty state — used whenever a query succeeds with zero results. */
export function EmptyState({ title, description, action, icon }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 rounded-lg border border-dashed border-slate-300 bg-white px-6 py-12 text-center">
      {icon ?? (
        <svg
          className="h-10 w-10 text-slate-300"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={1.5}
            d="M9 13h6m-3-3v6m9-3a9 9 0 11-18 0 9 9 0 0118 0z"
          />
        </svg>
      )}
      <h3 className="text-sm font-semibold text-slate-900">{title}</h3>
      {description && <p className="max-w-sm text-sm text-slate-500">{description}</p>}
      {action && <div className="mt-2">{action}</div>}
    </div>
  )
}

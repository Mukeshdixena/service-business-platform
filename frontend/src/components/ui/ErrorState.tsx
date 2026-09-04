import { getFriendlyErrorMessage } from '../../utils/apiError'
import { Button } from './Button'

export interface ErrorStateProps {
  error?: unknown
  title?: string
  onRetry?: () => void
}

/** Distinct, real error state — used whenever a query/mutation fails. */
export function ErrorState({ error, title = 'Something went wrong', onRetry }: ErrorStateProps) {
  const message = error ? getFriendlyErrorMessage(error) : undefined

  return (
    <div
      role="alert"
      className="flex flex-col items-center justify-center gap-2 rounded-lg border border-red-200 bg-red-50 px-6 py-10 text-center"
    >
      <svg className="h-8 w-8 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={1.5}
          d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z"
        />
      </svg>
      <h3 className="text-sm font-semibold text-red-800">{title}</h3>
      {message && <p className="max-w-sm text-sm text-red-700">{message}</p>}
      {onRetry && (
        <Button variant="secondary" size="sm" className="mt-2" onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  )
}

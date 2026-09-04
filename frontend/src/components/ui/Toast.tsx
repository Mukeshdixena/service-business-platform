import { cn } from '../../utils/cn'
import type { ToastVariant } from '../../hooks/useToast'

export interface ToastItemProps {
  message: string
  variant: ToastVariant
  onDismiss: () => void
}

const VARIANT_CLASSES: Record<ToastVariant, string> = {
  success: 'bg-green-600',
  error: 'bg-red-600',
  info: 'bg-slate-800',
}

/** A single toast pill. Rendered by ToastProvider's container — not used directly by feature code. */
export function ToastItem({ message, variant, onDismiss }: ToastItemProps) {
  return (
    <div
      role="status"
      className={cn(
        'flex items-center gap-3 rounded-md px-4 py-3 text-sm text-white shadow-lg',
        VARIANT_CLASSES[variant],
      )}
    >
      <span>{message}</span>
      <button
        type="button"
        onClick={onDismiss}
        aria-label="Dismiss notification"
        className="ml-auto text-white/80 hover:text-white"
      >
        ✕
      </button>
    </div>
  )
}

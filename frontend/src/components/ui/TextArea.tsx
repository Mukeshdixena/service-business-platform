import { forwardRef, useId } from 'react'
import type { TextareaHTMLAttributes } from 'react'
import { cn } from '../../utils/cn'

export interface TextAreaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string
  error?: string
  hint?: string
}

export const TextArea = forwardRef<HTMLTextAreaElement, TextAreaProps>(function TextArea(
  { label, error, hint, id, className, rows = 4, ...rest },
  ref,
) {
  const generatedId = useId()
  const areaId = id ?? generatedId
  const describedBy = error ? `${areaId}-error` : hint ? `${areaId}-hint` : undefined

  return (
    <div className="flex flex-col gap-1">
      {label && (
        <label htmlFor={areaId} className="text-sm font-medium text-slate-700">
          {label}
        </label>
      )}
      <textarea
        ref={ref}
        id={areaId}
        rows={rows}
        className={cn(
          'block w-full rounded-md border px-3 py-2 text-sm text-slate-900 shadow-sm focus:outline-none focus:ring-2',
          error
            ? 'border-red-400 focus:border-red-400 focus:ring-red-400'
            : 'border-slate-300 focus:border-brand-500 focus:ring-brand-500',
          className,
        )}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        {...rest}
      />
      {error && (
        <p id={`${areaId}-error`} className="text-sm text-red-600">
          {error}
        </p>
      )}
      {!error && hint && (
        <p id={`${areaId}-hint`} className="text-sm text-slate-500">
          {hint}
        </p>
      )}
    </div>
  )
})

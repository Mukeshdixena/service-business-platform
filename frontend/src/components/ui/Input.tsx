import { forwardRef, useId } from 'react'
import type { InputHTMLAttributes } from 'react'
import { cn } from '../../utils/cn'

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  hint?: string
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { label, error, hint, id, className, ...rest },
  ref,
) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  const describedBy = error ? `${inputId}-error` : hint ? `${inputId}-hint` : undefined

  return (
    <div className="flex flex-col gap-1">
      {label && (
        <label htmlFor={inputId} className="text-sm font-medium text-slate-700">
          {label}
        </label>
      )}
      <input
        ref={ref}
        id={inputId}
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
        <p id={`${inputId}-error`} className="text-sm text-red-600">
          {error}
        </p>
      )}
      {!error && hint && (
        <p id={`${inputId}-hint`} className="text-sm text-slate-500">
          {hint}
        </p>
      )}
    </div>
  )
})

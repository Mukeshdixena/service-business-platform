import { forwardRef, useId } from 'react'
import type { SelectHTMLAttributes } from 'react'
import { cn } from '../../utils/cn'

export interface SelectOption {
  value: string
  label: string
}

export interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string
  error?: string
  hint?: string
  options: SelectOption[]
  placeholder?: string
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select(
  { label, error, hint, id, className, options, placeholder, ...rest },
  ref,
) {
  const generatedId = useId()
  const selectId = id ?? generatedId
  const describedBy = error ? `${selectId}-error` : hint ? `${selectId}-hint` : undefined

  return (
    <div className="flex flex-col gap-1">
      {label && (
        <label htmlFor={selectId} className="text-sm font-medium text-slate-700">
          {label}
        </label>
      )}
      <select
        ref={ref}
        id={selectId}
        className={cn(
          'block w-full rounded-md border bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:outline-none focus:ring-2',
          error
            ? 'border-red-400 focus:border-red-400 focus:ring-red-400'
            : 'border-slate-300 focus:border-brand-500 focus:ring-brand-500',
          className,
        )}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        {...rest}
      >
        {placeholder && (
          <option value="" disabled>
            {placeholder}
          </option>
        )}
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {error && (
        <p id={`${selectId}-error`} className="text-sm text-red-600">
          {error}
        </p>
      )}
      {!error && hint && (
        <p id={`${selectId}-hint`} className="text-sm text-slate-500">
          {hint}
        </p>
      )}
    </div>
  )
})

import type { HTMLAttributes, TdHTMLAttributes, ThHTMLAttributes } from 'react'
import { cn } from '../../utils/cn'

/** Thin, styled wrappers around native table elements — no generic column/row abstraction (kept simple per §46). */

export function Table({ className, ...rest }: HTMLAttributes<HTMLTableElement>) {
  return (
    <div className="overflow-x-auto">
      <table className={cn('min-w-full divide-y divide-slate-200', className)} {...rest} />
    </div>
  )
}

export function THead({ className, ...rest }: HTMLAttributes<HTMLTableSectionElement>) {
  return <thead className={cn('bg-slate-50', className)} {...rest} />
}

export function TBody({ className, ...rest }: HTMLAttributes<HTMLTableSectionElement>) {
  return <tbody className={cn('divide-y divide-slate-100 bg-white', className)} {...rest} />
}

export function TR({ className, ...rest }: HTMLAttributes<HTMLTableRowElement>) {
  return <tr className={cn('', className)} {...rest} />
}

export function TH({ className, ...rest }: ThHTMLAttributes<HTMLTableCellElement>) {
  return (
    <th
      scope="col"
      className={cn(
        'px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500',
        className,
      )}
      {...rest}
    />
  )
}

export function TD({ className, ...rest }: TdHTMLAttributes<HTMLTableCellElement>) {
  return <td className={cn('whitespace-nowrap px-4 py-3 text-sm text-slate-700', className)} {...rest} />
}

import type { ReactNode } from 'react'
import { cn } from '../../utils/cn'

export interface CardProps {
  children: ReactNode
  className?: string
  title?: ReactNode
  actions?: ReactNode
}

export function Card({ children, className, title, actions }: CardProps) {
  return (
    <div className={cn('rounded-lg border border-slate-200 bg-white shadow-sm', className)}>
      {(title || actions) && (
        <div className="flex items-center justify-between gap-3 border-b border-slate-100 px-4 py-3 sm:px-6">
          {title && <h3 className="text-base font-semibold text-slate-900">{title}</h3>}
          {actions && <div className="flex items-center gap-2">{actions}</div>}
        </div>
      )}
      <div className="p-4 sm:p-6">{children}</div>
    </div>
  )
}

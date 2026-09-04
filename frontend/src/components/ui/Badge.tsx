import type { ReactNode } from 'react'
import { cn } from '../../utils/cn'

export type BadgeColor = 'slate' | 'green' | 'red' | 'amber' | 'blue' | 'purple'

const COLOR_CLASSES: Record<BadgeColor, string> = {
  slate: 'bg-slate-100 text-slate-700',
  green: 'bg-green-100 text-green-800',
  red: 'bg-red-100 text-red-800',
  amber: 'bg-amber-100 text-amber-800',
  blue: 'bg-blue-100 text-blue-800',
  purple: 'bg-purple-100 text-purple-800',
}

export interface BadgeProps {
  children: ReactNode
  color?: BadgeColor
  className?: string
}

/** Generic colored pill — no domain knowledge. See components/booking and
 *  components/business for the domain-specific status -> color mappings. */
export function Badge({ children, color = 'slate', className }: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
        COLOR_CLASSES[color],
        className,
      )}
    >
      {children}
    </span>
  )
}

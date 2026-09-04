import type { AvailabilitySlot } from '../../types'
import { formatTime } from '../../utils/date'
import { cn } from '../../utils/cn'

export interface SlotPickerProps {
  slots: AvailabilitySlot[]
  selected?: AvailabilitySlot | null
  onSelect: (slot: AvailabilitySlot) => void
}

/** Renders a grid of bookable time slots for a single day. Purely presentational — the caller owns the selected slot. */
export function SlotPicker({ slots, selected, onSelect }: SlotPickerProps) {
  return (
    <div
      role="listbox"
      aria-label="Available time slots"
      className="grid grid-cols-3 gap-2 sm:grid-cols-4 md:grid-cols-5"
    >
      {slots.map((slot) => {
        const isSelected = selected?.start === slot.start && selected.staffId === slot.staffId
        return (
          <button
            key={`${slot.start}-${slot.staffId ?? 'any'}`}
            type="button"
            role="option"
            aria-selected={isSelected}
            disabled={!slot.available}
            onClick={() => onSelect(slot)}
            className={cn(
              'rounded-md border px-2 py-2 text-sm font-medium transition-colors',
              !slot.available && 'cursor-not-allowed border-slate-100 bg-slate-50 text-slate-300 line-through',
              slot.available && !isSelected && 'border-slate-300 text-slate-700 hover:border-brand-400 hover:bg-brand-50',
              slot.available && isSelected && 'border-brand-600 bg-brand-600 text-white',
            )}
          >
            {formatTime(slot.start)}
          </button>
        )
      })}
    </div>
  )
}

import { useEffect } from 'react'
import type { ReactNode } from 'react'
import { createPortal } from 'react-dom'

export interface ModalProps {
  isOpen: boolean
  onClose: () => void
  title?: string
  children: ReactNode
  footer?: ReactNode
}

export function Modal({ isOpen, onClose, title, children, footer }: ModalProps) {
  useEffect(() => {
    if (!isOpen) return
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, onClose])

  if (!isOpen) return null

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center px-4">
      <button
        aria-label="Close dialog"
        className="fixed inset-0 bg-slate-900/50"
        onClick={onClose}
      />
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className="relative z-10 w-full max-w-lg rounded-lg bg-white shadow-xl"
      >
        {title && (
          <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
            <h2 className="text-base font-semibold text-slate-900">{title}</h2>
            <button
              type="button"
              onClick={onClose}
              aria-label="Close"
              className="rounded-md p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
            >
              ✕
            </button>
          </div>
        )}
        <div className="px-6 py-4">{children}</div>
        {footer && <div className="flex justify-end gap-2 border-t border-slate-100 px-6 py-4">{footer}</div>}
      </div>
    </div>,
    document.body,
  )
}

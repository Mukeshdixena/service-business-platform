export interface FormErrorProps {
  message?: string | null
}

/** Shared banner for surfacing a backend error at the top/bottom of a form (§47: never swallow backend validation). */
export function FormError({ message }: FormErrorProps) {
  if (!message) return null
  return (
    <div role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
      {message}
    </div>
  )
}

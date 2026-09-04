/** Formats a price + ISO-4217 currency code consistently across the app. */
export function formatMoney(price: number, currency: string): string {
  try {
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(price)
  } catch {
    // Unknown/invalid currency code — fall back to a plain "CODE amount" format
    // rather than throwing while rendering.
    return `${currency} ${price.toFixed(2)}`
  }
}

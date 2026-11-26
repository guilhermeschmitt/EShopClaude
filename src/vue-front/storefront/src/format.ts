// vue-base's currencyFormatter is fixed to pt-BR; this English UI formats prices as en-US dollars.
const priceFormat = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

/** `17` → `$17.00`. */
export function formatPrice(value: number): string {
  return priceFormat.format(value);
}

// vue-base's date formatters are pt-BR too; dates show in en-US, in the browser's timezone.
const dateFormat = new Intl.DateTimeFormat('en-US', { dateStyle: 'medium', timeStyle: 'short' });

/** ISO-8601 → `Nov 26, 2026, 3:04 PM` (in the browser's timezone). */
export function formatDate(iso: string): string {
  return dateFormat.format(new Date(iso));
}

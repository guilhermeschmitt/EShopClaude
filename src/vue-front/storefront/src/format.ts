// vue-base's currencyFormatter is fixed to pt-BR; this English UI formats prices as en-US dollars.
const priceFormat = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

/** `17` → `$17.00`. */
export function formatPrice(value: number): string {
  return priceFormat.format(value);
}

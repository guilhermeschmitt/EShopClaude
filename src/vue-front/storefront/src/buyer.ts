// The anonymous buyer identity (PROJECT.md §3). The only code that touches this localStorage key.
const BUYER_ID_KEY = 'eshop.buyerId';

/** Returns the buyerId, generating a UUID v4 and storing it on first use. */
export function getBuyerId(): string {
  let buyerId = localStorage.getItem(BUYER_ID_KEY);
  if (!buyerId) {
    buyerId = crypto.randomUUID();
    localStorage.setItem(BUYER_ID_KEY, buyerId);
  }
  return buyerId;
}

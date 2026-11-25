import { computed, readonly, ref } from 'vue';
import { getBuyerId } from '@/buyer';
import { showError } from '@/errors';
import { BasketService, type Basket } from '@/services/BasketService';

// Shared basket state: loaded once on start. Pages that change the basket either set it from the response
// (addItem) or call refreshBasket() afterwards. `status` tells a basket still loading from one that failed to load.
const state = ref<Basket | null>(null);

export type BasketStatus = 'loading' | 'ready' | 'failed';

const statusState = ref<BasketStatus>('loading');

/** Last basket loaded; null before the first load completes and after a failed load. */
export const basket = readonly(state);

/** Starts as 'loading'; refreshBasket sets 'ready' or 'failed', a successful addItem sets 'ready'. It never goes
 * back to 'loading', so a later refresh doesn't bring a skeleton back. */
export const status = readonly(statusState);

/** Sum of quantities over all lines (0 when nothing is loaded). */
export const itemCount = computed(() => state.value?.items.reduce((sum, item) => sum + item.quantity, 0) ?? 0);

// Created on first use: ApiService reads Core.baseURL, which is only set once Core.init has run.
let service: BasketService | undefined;

/** Reloads the basket. Never rejects: a failure clears the state and shows the error toast. */
export async function refreshBasket(): Promise<void> {
  service ??= new BasketService();
  try {
    state.value = await service.get(getBuyerId());
    statusState.value = 'ready';
  } catch (error) {
    state.value = null;
    statusState.value = 'failed';
    showError('Could not load the cart', error);
  }
}

/** Adds one unit of a product; the basket Basket returns becomes the state. Never rejects: a failure leaves the
 * state as it was and shows the error toast. */
export async function addItem(productId: number): Promise<void> {
  service ??= new BasketService();
  try {
    state.value = await service.addItem(getBuyerId(), productId);
    statusState.value = 'ready';
  } catch (error) {
    showError('Could not add to cart', error);
  }
}

export function useBasket() {
  return { basket, itemCount, status, refreshBasket, addItem };
}

import { computed, readonly, ref } from 'vue';
import { getBuyerId } from '@/buyer';
import { showError } from '@/errors';
import { BasketService, type Basket } from '@/services/BasketService';

// Shared basket state: loaded once on start; pages that change the basket call refreshBasket() afterwards.
const state = ref<Basket | null>(null);

/** Last basket loaded; null before the first load completes and after a failed load. */
export const basket = readonly(state);

/** Sum of quantities over all lines (0 when nothing is loaded). */
export const itemCount = computed(() => state.value?.items.reduce((sum, item) => sum + item.quantity, 0) ?? 0);

// Created on first use: ApiService reads Core.baseURL, which is only set once Core.init has run.
let service: BasketService | undefined;

/** Reloads the basket. Never rejects: a failure clears the state and shows the error toast. */
export async function refreshBasket(): Promise<void> {
  service ??= new BasketService();
  try {
    state.value = await service.get(getBuyerId());
  } catch (error) {
    state.value = null;
    showError('Could not load the cart', error);
  }
}

export function useBasket() {
  return { basket, itemCount, refreshBasket };
}

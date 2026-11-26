<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getBuyerId } from '@/buyer';
import LineItems from '@/components/LineItems.vue';
import OrderSummary from '@/components/OrderSummary.vue';
import { showError } from '@/errors';
import { BasketService } from '@/services/BasketService';
import { useBasket } from '@/stores/basket';

// Read-only review of the basket before ordering. It GETs the basket once on mount through the store, so it shows
// Basket's current lines and total. Place order POSTs the checkout once (disabled while it runs), then opens the new
// order and refreshes the badge; a failure shows a toast and keeps the page as it is.
const { basket, itemCount, status, refreshBasket } = useBasket();
const router = useRouter();
// Created in setup, after Core.init has set Core.baseURL.
const service = new BasketService();
const items = computed(() => basket.value?.items ?? []);
const total = computed(() => basket.value?.total ?? 0);
const placing = ref(false);

onMounted(() => {
  void refreshBasket();
});

async function onPlaceOrder(): Promise<void> {
  if (placing.value) return;
  placing.value = true;
  try {
    const orderId = await service.checkout(getBuyerId());
    await router.push(`/orders/${orderId}`);
    void refreshBasket();
  } catch (error) {
    showError('Could not place the order', error);
  } finally {
    placing.value = false;
  }
}
</script>

<template>
  <div class="page-head">
    <h1>Checkout</h1>
    <p v-if="status === 'ready' && items.length > 0" class="page-head__meta">Check your items, then place the order.</p>
  </div>

  <div v-if="status === 'loading'" class="shop-split">
    <ECard loading />
    <ECard loading />
  </div>

  <ECard v-else-if="status === 'ready' && items.length === 0">
    <EEmptyState icon="shopping_cart" title="Your cart is empty" description="There is nothing to check out yet.">
      <EButton label="Back to cart" to="/cart" outlined prepend-icon="arrow_back" class="checkout-back" />
    </EEmptyState>
  </ECard>

  <div v-else-if="status === 'ready'" class="shop-split">
    <LineItems :items="items" label="Order review" line-testid="review-line" />
    <div class="shop-split__aside">
      <OrderSummary heading="Order total" :item-count="itemCount" :total="total" total-testid="review-total">
        <EButton
          label="Place order"
          type="primary"
          size="large"
          prepend-icon="shopping_bag"
          loading-label="Placing order…"
          :loading="placing"
          :disabled="placing"
          data-testid="place-order"
          @click="onPlaceOrder"
        />
        <RouterLink to="/cart" class="shop-link checkout-edit">Edit cart</RouterLink>
      </OrderSummary>
    </div>
  </div>
</template>

<style scoped>
.checkout-edit {
  align-self: center;
}
</style>

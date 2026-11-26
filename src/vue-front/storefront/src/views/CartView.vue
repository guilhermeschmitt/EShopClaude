<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import LineItems from '@/components/LineItems.vue';
import OrderSummary from '@/components/OrderSummary.vue';
import { useBasket } from '@/stores/basket';

// Reads the shared basket (App.vue loads it on start) and never GETs it itself. +/- and remove send a PUT whose
// response becomes the shared basket, so the lines, total and header badge update from that one request.
// Checkout navigates to /checkout; it is disabled when the cart is empty or while a PUT runs.
const { basket, itemCount, status, updateQuantity } = useBasket();
const router = useRouter();
const items = computed(() => basket.value?.items ?? []);
const total = computed(() => basket.value?.total ?? 0);
const countLabel = computed(() => (itemCount.value === 1 ? '1 item' : `${itemCount.value} items`));

// One adjust at a time: while a PUT is in flight every +, - and remove button is disabled.
const updatingId = ref<number | null>(null);

async function onAdjust(productId: number, quantity: number): Promise<void> {
  if (updatingId.value !== null) return;
  updatingId.value = productId;
  try {
    await updateQuantity(productId, quantity);
  } finally {
    updatingId.value = null;
  }
}

function onCheckout(): void {
  void router.push('/checkout');
}
</script>

<template>
  <div class="page-head">
    <h1>Cart</h1>
    <p v-if="status === 'ready' && items.length > 0" class="page-head__meta num">{{ countLabel }}</p>
  </div>

  <div v-if="status === 'loading'" class="shop-split">
    <ECard loading />
    <ECard loading />
  </div>

  <ECard v-else-if="status === 'ready' && items.length === 0">
    <EEmptyState icon="shopping_cart" title="Your cart is empty" description="Add a few products and they will show up here.">
      <EButton label="Browse products" to="/" prepend-icon="storefront" class="cart-browse" />
    </EEmptyState>
  </ECard>

  <div v-else-if="status === 'ready'" class="shop-split">
    <LineItems
      :items="items"
      label="Cart lines"
      line-testid="cart-line"
      editable
      :disabled="updatingId !== null"
      @adjust="onAdjust"
    />
    <div class="shop-split__aside">
      <OrderSummary :item-count="itemCount" :total="total" total-testid="cart-total">
        <EButton
          label="Checkout"
          type="primary"
          size="large"
          prepend-icon="shopping_cart_checkout"
          :disabled="items.length === 0 || updatingId !== null"
          data-testid="checkout-button"
          @click="onCheckout"
        />
        <RouterLink to="/" class="shop-link cart-continue">Continue shopping</RouterLink>
      </OrderSummary>
    </div>
  </div>
</template>

<style scoped>
.cart-continue {
  align-self: center;
}
</style>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import LineItems from '@/components/LineItems.vue';
import OrderSummary from '@/components/OrderSummary.vue';
import { useBasket } from '@/stores/basket';

// Reads the shared basket (App.vue loads it on start) and never GETs it itself. +/- and remove send a PUT whose
// response becomes the shared basket, so the lines, total and header badge update from that one request.
// Checkout navigates to /checkout; it is disabled when the cart is empty or while a PUT runs.
const { basket, itemCount, status, updateQuantity, addItem, refreshBasket } = useBasket();
const router = useRouter();
const items = computed(() => basket.value?.items ?? []);
const total = computed(() => basket.value?.total ?? 0);
const countLabel = computed(() => (itemCount.value === 1 ? '1 item' : `${itemCount.value} items`));

// One adjust at a time: while a PUT is in flight every +, - and remove button is disabled.
const updatingId = ref<number | null>(null);

// The last line removed (by the trash button or by − at 1), so it can be put back. Cleared by any other change.
const removed = ref<{ productId: number; productName: string; quantity: number } | null>(null);

async function onAdjust(productId: number, quantity: number): Promise<void> {
  if (updatingId.value !== null) return;
  const line = items.value.find((item) => item.productId === productId);
  updatingId.value = productId;
  try {
    await updateQuantity(productId, quantity);
    const gone = !items.value.some((item) => item.productId === productId);
    removed.value = quantity === 0 && gone && line ? { ...line } : null;
  } finally {
    updatingId.value = null;
  }
}

/** Undo a removal: add the product back (one POST), then restore its quantity when it was more than one. */
async function onUndo(): Promise<void> {
  const line = removed.value;
  if (!line || updatingId.value !== null) return;
  updatingId.value = line.productId;
  try {
    if (await addItem(line.productId)) {
      removed.value = null;
      if (line.quantity > 1) await updateQuantity(line.productId, line.quantity);
    }
  } finally {
    updatingId.value = null;
  }
}

// Retry after a failed load: the store keeps status "failed" until the GET succeeds.
const retrying = ref(false);

async function onRetry(): Promise<void> {
  retrying.value = true;
  try {
    await refreshBasket();
  } finally {
    retrying.value = false;
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

  <div role="status">
    <p v-if="removed" class="cart-removed" data-testid="removed-notice">
      <EIcon icon="delete" aria-hidden="true" />
      <span class="cart-removed__text">Removed {{ removed.productName }}.</span>
      <EButton label="Undo" outlined size="small" :disabled="updatingId !== null" data-testid="undo-remove" @click="onUndo" />
    </p>
  </div>

  <div v-if="status === 'loading'" class="shop-split">
    <ECard loading />
    <ECard loading />
  </div>

  <ECard v-else-if="status === 'failed'">
    <EEmptyState icon="cloud_off" title="Your cart didn’t load" description="The cart couldn’t be reached. Check your connection, then try again.">
      <EButton label="Try again" prepend-icon="refresh" :loading="retrying" loading-label="Trying again…" data-testid="retry" @click="onRetry" />
    </EEmptyState>
  </ECard>

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
.cart-removed {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--e-space-3);
  margin: 0 0 var(--e-space-4);
  padding: var(--e-space-3) var(--e-space-4);
  border: 1px solid var(--e-color-border);
  border-radius: var(--e-radius-md);
  background: var(--e-color-surface);
  font-weight: 600;
}
.cart-removed__text {
  flex: 1 1 12rem;
  min-width: 0;
}
.cart-removed :deep(.e-button) {
  min-height: var(--shop-target);
  min-width: 5rem;
}
</style>

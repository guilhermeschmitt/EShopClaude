<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getBuyerId } from '@/buyer';
import { showError } from '@/errors';
import { formatPrice } from '@/format';
import { BasketService } from '@/services/BasketService';
import { useBasket } from '@/stores/basket';

// Read-only review of the basket before ordering. It GETs the basket once on mount through the store, so it shows
// Basket's current lines and total. Place order POSTs the checkout once (disabled while it runs), then opens the new
// order and refreshes the badge; a failure shows a toast and keeps the page as it is.
const { basket, status, refreshBasket } = useBasket();
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
  <h1>Checkout</h1>

  <ECard v-if="status === 'loading'" loading />

  <template v-else-if="status === 'ready' && items.length === 0">
    <EEmptyState icon="shopping_cart" title="Your cart is empty" />
    <RouterLink to="/cart" class="checkout-back">Back to cart</RouterLink>
  </template>

  <ECard v-else-if="status === 'ready'">
    <table class="cart-table" aria-label="Order review">
      <thead>
        <tr>
          <th scope="col">Product</th>
          <th scope="col" class="cart-table__num">Unit price</th>
          <th scope="col" class="cart-table__num">Quantity</th>
          <th scope="col" class="cart-table__num">Line total</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.productId" data-testid="review-line">
          <td>{{ item.productName }}</td>
          <td class="cart-table__num">{{ formatPrice(item.unitPrice) }}</td>
          <td class="cart-table__num" data-testid="line-quantity">{{ item.quantity }}</td>
          <td class="cart-table__num" data-testid="line-total">{{ formatPrice(item.unitPrice * item.quantity) }}</td>
        </tr>
      </tbody>
      <tfoot>
        <tr>
          <th scope="row" colspan="3">Total</th>
          <td class="cart-table__num" data-testid="review-total">{{ formatPrice(total) }}</td>
        </tr>
      </tfoot>
    </table>
  </ECard>

  <div v-if="status === 'ready' && items.length > 0" class="checkout-actions">
    <EButton
      label="Place order"
      type="primary"
      prepend-icon="shopping_bag"
      loading-label="Placing order…"
      :loading="placing"
      :disabled="placing"
      data-testid="place-order"
      @click="onPlaceOrder"
    />
  </div>
</template>

<style scoped>
.cart-table {
  width: 100%;
  border-collapse: collapse;
}
.cart-table th,
.cart-table td {
  padding: 0.5rem 0.75rem;
  text-align: left;
  vertical-align: middle;
}
.cart-table tr {
  border-bottom: 1px solid var(--e-primitive-slate-200, #dee1e7);
}
.cart-table tfoot tr {
  border-bottom: none;
}
.cart-table .cart-table__num {
  text-align: right;
}
.cart-table tfoot {
  font-weight: 700;
}
.checkout-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
}
.checkout-back {
  display: inline-block;
  margin-top: 0.75rem;
}
</style>

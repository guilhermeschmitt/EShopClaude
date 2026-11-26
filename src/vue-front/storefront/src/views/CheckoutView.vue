<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { formatPrice } from '@/format';
import { useBasket } from '@/stores/basket';

// Read-only review of the basket before ordering. It GETs the basket once on mount through the store, so it shows
// Basket's current lines and total; nothing here changes the basket. storefront/13 adds Place order.
const { basket, status, refreshBasket } = useBasket();
const items = computed(() => basket.value?.items ?? []);
const total = computed(() => basket.value?.total ?? 0);

onMounted(() => {
  void refreshBasket();
});
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
.checkout-back {
  display: inline-block;
  margin-top: 0.75rem;
}
</style>

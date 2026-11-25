<script setup lang="ts">
import { computed } from 'vue';
import { formatPrice } from '@/format';
import { useBasket } from '@/stores/basket';

// Reads the shared basket only: App.vue loads it on start and addItem replaces it, so this page sends no request.
const { basket, status } = useBasket();
const items = computed(() => basket.value?.items ?? []);
const total = computed(() => basket.value?.total ?? 0);
</script>

<template>
  <h1>Cart</h1>

  <ECard v-if="status === 'loading'" loading />

  <template v-else-if="status === 'ready' && items.length === 0">
    <EEmptyState icon="shopping_cart" title="Your cart is empty" />
    <RouterLink to="/" class="cart-browse">Browse products</RouterLink>
  </template>

  <ECard v-else-if="status === 'ready'">
    <table class="cart-table" aria-label="Cart lines">
      <thead>
        <tr>
          <th scope="col">Product</th>
          <th scope="col" class="cart-table__num">Unit price</th>
          <th scope="col" class="cart-table__num">Quantity</th>
          <th scope="col" class="cart-table__num">Line total</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.productId" data-testid="cart-line">
          <td>{{ item.productName }}</td>
          <td class="cart-table__num">{{ formatPrice(item.unitPrice) }}</td>
          <td class="cart-table__num">{{ item.quantity }}</td>
          <td class="cart-table__num" data-testid="line-total">{{ formatPrice(item.unitPrice * item.quantity) }}</td>
        </tr>
      </tbody>
      <tfoot>
        <tr>
          <th scope="row" colspan="3">Total</th>
          <td class="cart-table__num" data-testid="cart-total">{{ formatPrice(total) }}</td>
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
.cart-browse {
  display: inline-block;
  margin-top: 0.75rem;
}
</style>

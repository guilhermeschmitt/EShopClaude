<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { getBuyerId } from '@/buyer';
import { showError } from '@/errors';
import { formatDate, formatPrice } from '@/format';
import { OrderingService, type OrderSummary } from '@/services/OrderingService';

// This buyer's orders: one GET to Ordering on mount. Rows keep Ordering's order (newest first), and each one links
// to its detail page (storefront/12).
const orders = ref<OrderSummary[]>([]);
const loading = ref(true);
const failed = ref(false);

onMounted(async () => {
  // Created here, not at module load: ApiService reads Core.baseURL, which is only set once Core.init has run.
  const service = new OrderingService();
  try {
    orders.value = await service.listByBuyer(getBuyerId());
  } catch (error) {
    failed.value = true;
    showError('Could not load your orders', error);
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <h1>My orders</h1>

  <ECard v-if="loading" loading />

  <template v-else-if="!failed && orders.length === 0">
    <EEmptyState icon="receipt_long" title="No orders yet" />
    <RouterLink to="/" class="orders-browse">Browse products</RouterLink>
  </template>

  <ECard v-else-if="!failed">
    <table class="orders-table" aria-label="Orders">
      <thead>
        <tr>
          <th scope="col">Order</th>
          <th scope="col">Date</th>
          <th scope="col" class="orders-table__num">Items</th>
          <th scope="col" class="orders-table__num">Total</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="order in orders" :key="order.id" data-testid="order-row">
          <td>
            <RouterLink :to="`/orders/${order.id}`" data-testid="order-link">Order #{{ order.id }}</RouterLink>
          </td>
          <td data-testid="order-date">{{ formatDate(order.createdAt) }}</td>
          <td class="orders-table__num" data-testid="order-items">{{ order.itemCount }}</td>
          <td class="orders-table__num" data-testid="order-total">{{ formatPrice(order.total) }}</td>
        </tr>
      </tbody>
    </table>
  </ECard>
</template>

<style scoped>
.orders-table {
  width: 100%;
  border-collapse: collapse;
}
.orders-table th,
.orders-table td {
  padding: 0.5rem 0.75rem;
  text-align: left;
  vertical-align: middle;
}
.orders-table tr {
  border-bottom: 1px solid var(--e-primitive-slate-200, #dee1e7);
}
.orders-table .orders-table__num {
  text-align: right;
}
.orders-browse {
  display: inline-block;
  margin-top: 0.75rem;
}
</style>

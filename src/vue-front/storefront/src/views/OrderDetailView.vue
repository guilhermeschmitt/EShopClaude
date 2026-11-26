<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { showError } from '@/errors';
import { formatDate, formatPrice } from '@/format';
import { OrderingService, type Order } from '@/services/OrderingService';

// One order: one GET to Ordering on mount. Lines, unit prices and the total are shown as Ordering returns them (the
// snapshot taken at checkout). A 404 (unknown id) or 400 (non-numeric id) means "not found"; anything else is an error.
const id = String(useRoute().params.id);
const order = ref<Order | null>(null);
const loading = ref(true);
const notFound = ref(false);
const failed = ref(false);

function httpStatus(error: unknown): number | undefined {
  if (typeof error === 'object' && error !== null) {
    return (error as { response?: { status?: number } }).response?.status;
  }
  return undefined;
}

onMounted(async () => {
  // Created here, not at module load: ApiService reads Core.baseURL, which is only set once Core.init has run.
  const service = new OrderingService();
  try {
    order.value = await service.get(id);
  } catch (error) {
    const status = httpStatus(error);
    if (status === 404 || status === 400) {
      notFound.value = true;
    } else {
      failed.value = true;
      showError('Could not load the order', error);
    }
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <h1>Order #{{ id }}</h1>

  <ECard v-if="loading" loading />

  <EEmptyState v-else-if="notFound" icon="receipt_long" title="Order not found" />

  <template v-else-if="!failed && order">
    <p class="order-date" data-testid="order-date">Placed {{ formatDate(order.createdAt) }}</p>
    <ECard>
      <table class="cart-table" aria-label="Order lines">
        <thead>
          <tr>
            <th scope="col">Product</th>
            <th scope="col" class="cart-table__num">Unit price</th>
            <th scope="col" class="cart-table__num">Quantity</th>
            <th scope="col" class="cart-table__num">Line total</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, i) in order.items" :key="i" data-testid="order-line">
            <td>{{ item.productName }}</td>
            <td class="cart-table__num" data-testid="line-unit-price">{{ formatPrice(item.unitPrice) }}</td>
            <td class="cart-table__num" data-testid="line-quantity">{{ item.quantity }}</td>
            <td class="cart-table__num" data-testid="line-total">{{ formatPrice(item.unitPrice * item.quantity) }}</td>
          </tr>
        </tbody>
        <tfoot>
          <tr>
            <th scope="row" colspan="3">Total</th>
            <td class="cart-table__num" data-testid="order-total">{{ formatPrice(order.total) }}</td>
          </tr>
        </tfoot>
      </table>
    </ECard>
  </template>

  <RouterLink to="/orders" class="order-back" data-testid="order-back">Back to my orders</RouterLink>
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
.order-date {
  margin: 0 0 0.75rem;
  color: var(--e-color-text-secondary, #5b6271);
}
.order-back {
  display: inline-block;
  margin-top: 0.75rem;
}
</style>

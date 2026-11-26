<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { getBuyerId } from '@/buyer';
import { showError } from '@/errors';
import { formatDate, formatPrice } from '@/format';
import { OrderingService, type OrderSummary } from '@/services/OrderingService';

// This buyer's orders: one GET to Ordering on mount. Rows keep Ordering's order (newest first), and each one links
// to its detail page (storefront/12).
const orders = ref<OrderSummary[]>([]);
const loading = ref(true);
const failed = ref(false);
const countLabel = computed(() => (orders.value.length === 1 ? '1 order' : `${orders.value.length} orders`));

async function load(): Promise<void> {
  // Created here, not at module load: ApiService reads Core.baseURL, which is only set once Core.init has run.
  const service = new OrderingService();
  loading.value = true;
  failed.value = false;
  try {
    orders.value = await service.listByBuyer(getBuyerId());
  } catch (error) {
    failed.value = true;
    showError('Could not load your orders', error);
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="page-head">
    <h1>My orders</h1>
    <p v-if="!loading && !failed && orders.length > 0" class="page-head__meta num">{{ countLabel }}</p>
  </div>

  <ECard v-if="loading" loading />

  <ECard v-else-if="failed">
    <EEmptyState
      icon="cloud_off"
      title="Your orders didn’t load"
      description="Orders couldn’t be reached. Check your connection, then try again."
    >
      <EButton label="Try again" prepend-icon="refresh" data-testid="retry" @click="load" />
    </EEmptyState>
  </ECard>

  <ECard v-else-if="orders.length === 0">
    <EEmptyState icon="receipt_long" title="No orders yet" description="Orders you place will be listed here.">
      <EButton label="Browse products" to="/" prepend-icon="storefront" class="orders-browse" />
    </EEmptyState>
  </ECard>

  <ECard v-else flush>
    <table class="orders-table" role="table" aria-label="Orders">
      <thead role="rowgroup">
        <tr role="row">
          <th scope="col" role="columnheader">Order</th>
          <th scope="col" role="columnheader">Date</th>
          <th scope="col" role="columnheader" class="orders-table__num">Items</th>
          <th scope="col" role="columnheader" class="orders-table__num">Total</th>
          <th scope="col" role="columnheader" class="orders-table__chevron"><span class="visually-hidden">Open</span></th>
        </tr>
      </thead>
      <tbody role="rowgroup">
        <tr v-for="order in orders" :key="order.id" role="row" class="orders-table__row" data-testid="order-row">
          <th scope="row" role="rowheader" class="orders-table__id">
            <RouterLink :to="`/orders/${order.id}`" class="orders-table__link" data-testid="order-link">
              Order <span class="num">#{{ order.id }}</span>
            </RouterLink>
          </th>
          <td role="cell" class="orders-table__date" data-testid="order-date">{{ formatDate(order.createdAt) }}</td>
          <td role="cell" class="orders-table__num orders-table__items">
            <span class="num" data-testid="order-items">{{ order.itemCount }}</span>
            <span class="orders-table__unit">&nbsp;{{ order.itemCount === 1 ? 'item' : 'items' }}</span>
          </td>
          <td role="cell" class="orders-table__num orders-table__total" data-testid="order-total">
            <span class="num">{{ formatPrice(order.total) }}</span>
          </td>
          <td role="cell" class="orders-table__chevron" aria-hidden="true">
            <EIcon icon="chevron_right" />
          </td>
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
  padding: var(--e-space-4);
  text-align: left;
  vertical-align: middle;
}
.orders-table thead th {
  padding-block: var(--e-space-3);
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--e-color-text-secondary);
  background: var(--e-color-surface-hover);
  white-space: nowrap;
}
.orders-table thead th:first-child {
  border-top-left-radius: calc(var(--e-radius-lg) - 1px);
}
.orders-table thead th:last-child {
  border-top-right-radius: calc(var(--e-radius-lg) - 1px);
}
.orders-table__row {
  position: relative;
  transition: background-color var(--e-transition-fast);
}
.orders-table__row + .orders-table__row {
  border-top: 1px solid var(--e-color-border);
}
.orders-table__row:hover {
  background: var(--e-color-surface-hover);
}
.orders-table__link {
  font-family: var(--e-font-family-heading);
  font-size: 1.0625rem;
  font-weight: 600;
  text-decoration: none;
}
/* The link covers the whole row, so the row is one big target. */
.orders-table__link::after {
  content: '';
  position: absolute;
  inset: 0;
}
.orders-table__link:focus-visible {
  outline: none;
}
.orders-table__link:focus-visible::after {
  outline: 2px solid var(--e-color-primary);
  outline-offset: -2px;
  border-radius: var(--e-radius-sm);
}
.orders-table__date {
  color: var(--e-color-text-secondary);
}
.orders-table .orders-table__num {
  text-align: right;
  white-space: nowrap;
}
.orders-table__unit {
  display: none;
}
.orders-table__total {
  font-size: 1.0625rem;
  font-weight: 700;
}
.orders-table__chevron {
  width: 1%;
  padding-left: 0;
  color: var(--e-color-text-secondary);
}

/* Phones: each order becomes a two-line card (number and total, then date and item count). */
@media (max-width: 639.98px) {
  .orders-table thead {
    position: absolute;
    width: 1px;
    height: 1px;
    overflow: hidden;
    clip-path: inset(50%);
  }
  .orders-table__row {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto auto;
    grid-template-areas:
      'id total chevron'
      'date items chevron';
    column-gap: var(--e-space-3);
    row-gap: var(--e-space-1);
    padding: var(--e-space-4);
  }
  .orders-table tbody th,
  .orders-table tbody td {
    padding: 0;
  }
  .orders-table__id {
    grid-area: id;
  }
  .orders-table__total {
    grid-area: total;
  }
  .orders-table__date {
    grid-area: date;
    font-size: 0.9375rem;
  }
  .orders-table__items {
    grid-area: items;
    font-size: 0.9375rem;
    color: var(--e-color-text-secondary);
  }
  .orders-table__unit {
    display: inline;
  }
  .orders-table__chevron {
    grid-area: chevron;
    align-self: center;
    width: auto;
  }
}
</style>

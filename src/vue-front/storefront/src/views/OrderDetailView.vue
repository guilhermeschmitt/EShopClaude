<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import LineItems from '@/components/LineItems.vue';
import OrderSummary from '@/components/OrderSummary.vue';
import { showError } from '@/errors';
import { formatDate } from '@/format';
import { OrderingService, type Order } from '@/services/OrderingService';

// One order: one GET to Ordering on mount. Lines, unit prices and the total are shown as Ordering returns them (the
// snapshot taken at checkout). A 404 (unknown id) or 400 (non-numeric id) means "not found"; anything else is an error,
// with a Try again that repeats the GET.
const id = String(useRoute().params.id);
const order = ref<Order | null>(null);
const loading = ref(true);
const notFound = ref(false);
const failed = ref(false);
const itemCount = computed(() => order.value?.items.reduce((sum, item) => sum + item.quantity, 0) ?? 0);

// Checkout navigates here with history state `placed`, so the order just placed gets a confirmation. Opening the
// same order later from My orders shows it without one; a reload of this history entry keeps it.
const justPlaced = (window.history.state as { placed?: unknown } | null)?.placed === true;

function httpStatus(error: unknown): number | undefined {
  if (typeof error === 'object' && error !== null) {
    return (error as { response?: { status?: number } }).response?.status;
  }
  return undefined;
}

async function load(): Promise<void> {
  // Created here, not at module load: ApiService reads Core.baseURL, which is only set once Core.init has run.
  const service = new OrderingService();
  loading.value = true;
  failed.value = false;
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
}

onMounted(load);
</script>

<template>
  <RouterLink to="/orders" class="shop-link order-back" data-testid="order-back">
    <EIcon icon="arrow_back" aria-hidden="true" />Back to my orders
  </RouterLink>

  <div class="page-head">
    <h1>Order <span class="num">#{{ id }}</span></h1>
    <p v-if="!loading && !failed && order" class="page-head__meta" data-testid="order-date">
      Placed {{ formatDate(order.createdAt) }}
    </p>
  </div>

  <div v-if="justPlaced" class="order-placed" role="status">
    <ECard v-if="order" data-testid="order-placed">
      <div class="order-placed__body">
        <EIcon icon="check_circle" class="order-placed__icon" aria-hidden="true" />
        <div class="order-placed__text">
          <p class="order-placed__title">Order placed</p>
          <p>Thanks! Your order is saved and listed in My orders.</p>
        </div>
        <EButton label="Continue shopping" to="/" outlined class="order-placed__action" />
      </div>
    </ECard>
  </div>

  <div v-if="loading" class="shop-split">
    <ECard loading />
    <ECard loading />
  </div>

  <ECard v-else-if="notFound">
    <EEmptyState
      icon="receipt_long"
      title="Order not found"
      description="There is no order with this number. Open the order from your list instead."
    >
      <EButton label="Go to my orders" to="/orders" outlined prepend-icon="receipt_long" />
    </EEmptyState>
  </ECard>

  <ECard v-else-if="failed">
    <EEmptyState
      icon="cloud_off"
      title="This order didn't load"
      description="Orders couldn't be reached. Check your connection, then try again."
    >
      <EButton label="Try again" prepend-icon="refresh" data-testid="retry" @click="load" />
    </EEmptyState>
  </ECard>

  <div v-else-if="order" class="shop-split">
    <LineItems :items="order.items" label="Order lines" line-testid="order-line" />
    <div class="shop-split__aside">
      <OrderSummary heading="Order total" :item-count="itemCount" :total="order.total" total-testid="order-total" />
    </div>
  </div>
</template>

<style scoped>
.order-back {
  margin: calc(-1 * var(--e-space-2)) 0 var(--e-space-2);
}
/* The confirmation re-tints vue-base's card through its own tokens. */
.order-placed {
  --e-color-surface: var(--e-color-success-subtle);
  --e-color-border: color-mix(in srgb, var(--e-color-success) 35%, transparent);
}
.order-placed > :deep(.e-card) {
  margin-bottom: var(--e-space-6);
}
.order-placed :deep(.e-card__body) {
  padding: var(--e-space-4) var(--e-space-5);
}
.order-placed__body {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--e-space-3) var(--e-space-4);
}
.order-placed__icon {
  font-size: 2rem;
  color: var(--e-color-success);
}
.order-placed__text {
  flex: 1 1 14rem;
  min-width: 0;
}
.order-placed__title {
  font-family: var(--e-font-family-heading);
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--e-color-success);
}
.order-placed__action {
  min-height: var(--shop-target);
}
</style>

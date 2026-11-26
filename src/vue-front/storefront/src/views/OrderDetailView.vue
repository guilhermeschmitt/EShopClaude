<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import LineItems from '@/components/LineItems.vue';
import OrderSummary from '@/components/OrderSummary.vue';
import { showError } from '@/errors';
import { formatDate } from '@/format';
import { OrderingService, type Order } from '@/services/OrderingService';

// One order: one GET to Ordering on mount. Lines, unit prices and the total are shown as Ordering returns them (the
// snapshot taken at checkout). A 404 (unknown id) or 400 (non-numeric id) means "not found"; anything else is an error.
const id = String(useRoute().params.id);
const order = ref<Order | null>(null);
const loading = ref(true);
const notFound = ref(false);
const failed = ref(false);
const itemCount = computed(() => order.value?.items.reduce((sum, item) => sum + item.quantity, 0) ?? 0);

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
  <RouterLink to="/orders" class="shop-link order-back" data-testid="order-back">
    <EIcon icon="arrow_back" aria-hidden="true" />Back to my orders
  </RouterLink>

  <div class="page-head">
    <h1>Order <span class="num">#{{ id }}</span></h1>
    <p v-if="!loading && !failed && order" class="page-head__meta" data-testid="order-date">
      Placed {{ formatDate(order.createdAt) }}
    </p>
  </div>

  <div v-if="loading" class="shop-split">
    <ECard loading />
    <ECard loading />
  </div>

  <ECard v-else-if="notFound">
    <EEmptyState
      icon="receipt_long"
      title="Order not found"
      description="Check the order number, or open the order from your list."
    />
  </ECard>

  <div v-else-if="!failed && order" class="shop-split">
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
</style>

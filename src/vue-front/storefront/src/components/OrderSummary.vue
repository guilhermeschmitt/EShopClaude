<script setup lang="ts">
import { useId } from 'vue';
import { formatPrice } from '@/format';

/** The receipt panel next to a line list: item count, total and the page's actions (default slot). */
defineProps<{ itemCount: number; total: number; totalTestid: string; heading?: string }>();
const headingId = useId();
</script>

<template>
  <section class="order-summary" :aria-labelledby="headingId">
    <ECard>
      <h2 :id="headingId" class="order-summary__heading">{{ heading ?? 'Summary' }}</h2>
      <dl class="order-summary__rows">
        <div class="order-summary__row">
          <dt>Items</dt>
          <dd class="num">{{ itemCount }}</dd>
        </div>
        <div class="order-summary__row order-summary__row--total">
          <dt>Total</dt>
          <dd class="num" :data-testid="totalTestid">{{ formatPrice(total) }}</dd>
        </div>
      </dl>
      <div v-if="$slots.default" class="order-summary__actions">
        <slot />
      </div>
    </ECard>
  </section>
</template>

<style scoped>
.order-summary__heading {
  font-size: 1.25rem;
  font-weight: 600;
}
.order-summary__rows {
  margin: 0;
}
.order-summary__row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--e-space-4);
  padding-block: var(--e-space-2);
}
.order-summary__row dt {
  font-weight: 600;
  color: var(--e-color-text-secondary);
}
.order-summary__row dd {
  margin: 0;
  font-size: 1.0625rem;
  font-weight: 600;
}
.order-summary__row--total {
  margin-top: var(--e-space-2);
  padding-top: var(--e-space-4);
  border-top: 1px dashed var(--e-color-border-strong);
}
.order-summary__row--total dt {
  font-size: 1.125rem;
  color: var(--e-color-text);
}
.order-summary__row--total dd {
  font-size: 1.75rem;
  font-weight: 700;
  letter-spacing: var(--e-letter-spacing-tight);
}
.order-summary__actions {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: var(--e-space-2);
}
.order-summary__actions :deep(.e-button) {
  width: 100%;
  min-height: 3rem;
}
</style>

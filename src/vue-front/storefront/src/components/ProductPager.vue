<script setup lang="ts">
import { useId } from 'vue';

/** Numbered page buttons. `page` is 1-based; the parent owns the page (in the URL) and reacts to `select`. */
defineProps<{ page: number; totalPages: number; disabled: boolean }>();
const emit = defineEmits<{ select: [page: number] }>();

// EButton always sets its own aria-label (only for icon-only buttons), overriding ours,
// so each button is named "Page N" through aria-labelledby on a hidden label.
const idPrefix = useId();
const labelId = (n: number): string => `${idPrefix}-page-${n}`;
</script>

<template>
  <nav class="product-pager" aria-label="Pagination">
    <template v-for="n in totalPages" :key="n">
      <span :id="labelId(n)" hidden>Page {{ n }}</span>
      <EButton
        :label="String(n)"
        :aria-labelledby="labelId(n)"
        :aria-current="n === page ? 'page' : undefined"
        :outlined="n !== page"
        class="product-pager__page num"
        :disabled="disabled"
        data-testid="pager-page"
        @click="emit('select', n)"
      />
    </template>
  </nav>
</template>

<style scoped>
.product-pager {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--e-space-2);
  margin-top: var(--e-space-8);
}
.product-pager__page {
  min-width: var(--shop-target);
  min-height: var(--shop-target);
  font-weight: 700;
}
</style>

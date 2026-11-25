<script setup lang="ts">
/** Numbered page buttons. `page` is 1-based; the parent owns the page (in the URL) and reacts to `select`. */
defineProps<{ page: number; totalPages: number; disabled: boolean }>();
const emit = defineEmits<{ select: [page: number] }>();
</script>

<template>
  <nav class="product-pager" aria-label="Pagination">
    <EButton
      v-for="n in totalPages"
      :key="n"
      :label="String(n)"
      :aria-label="`Page ${n}`"
      :aria-current="n === page ? 'page' : undefined"
      :outlined="n !== page"
      size="small"
      :disabled="disabled"
      data-testid="pager-page"
      @click="emit('select', n)"
    />
  </nav>
</template>

<style scoped>
.product-pager {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 0.5rem;
  margin-top: 1.5rem;
}
</style>

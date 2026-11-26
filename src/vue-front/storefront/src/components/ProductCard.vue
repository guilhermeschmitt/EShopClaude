<script setup lang="ts">
import { computed } from 'vue';
import { categoryLook } from '@/category';
import { formatPrice } from '@/format';
import type { Product } from '@/services/CatalogService';

/** One product: its category colour block, name, description, price and Add to cart. The parent owns the add. */
const props = defineProps<{ product: Product; adding: boolean; disabled: boolean }>();
const emit = defineEmits<{ add: [productId: number] }>();

const look = computed(() => categoryLook(props.product.category));
</script>

<template>
  <ECard flush class="product-card" :class="`product-card--${look.key}`">
    <div class="product-card__tile" aria-hidden="true">
      <EIcon :icon="look.icon" class="product-card__icon" />
    </div>
    <div class="product-card__body">
      <p class="product-card__category">{{ product.category }}</p>
      <h2 class="product-card__name">{{ product.name }}</h2>
      <p v-if="product.description" class="product-card__description">{{ product.description }}</p>
      <div class="product-card__buy">
        <p class="product-card__price num" data-testid="product-price">{{ formatPrice(product.price) }}</p>
        <EButton
          class="product-card__add"
          label="Add to cart"
          prepend-icon="add_shopping_cart"
          loading-label="Adding…"
          :aria-label="adding ? undefined : `Add to cart: ${product.name}`"
          :loading="adding"
          :disabled="disabled"
          data-testid="add-to-cart"
          @click="emit('add', product.id)"
        />
      </div>
    </div>
  </ECard>
</template>

<style scoped>
.product-card {
  --tint: var(--shop-cat-other-tint);
  --ink: var(--shop-cat-other-ink);
  height: 100%;
  overflow: hidden;
  transition: transform var(--e-transition-base), box-shadow var(--e-transition-base),
    border-color var(--e-transition-base);
}
.product-card--apparel {
  --tint: var(--shop-cat-apparel-tint);
  --ink: var(--shop-cat-apparel-ink);
}
.product-card--bags {
  --tint: var(--shop-cat-bags-tint);
  --ink: var(--shop-cat-bags-ink);
}
.product-card--drinkware {
  --tint: var(--shop-cat-drinkware-tint);
  --ink: var(--shop-cat-drinkware-ink);
}
.product-card--stationery {
  --tint: var(--shop-cat-stationery-tint);
  --ink: var(--shop-cat-stationery-ink);
}
.product-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--e-shadow-md);
  border-color: var(--e-color-border-strong);
}
.product-card :deep(.e-card__body) {
  display: grid;
  grid-template-columns: 6.5rem minmax(0, 1fr);
  gap: 0;
  height: 100%;
}
.product-card__tile {
  display: grid;
  place-items: center;
  background: var(--tint);
  color: var(--ink);
}
.product-card__icon {
  font-size: 2.75rem;
}
.product-card__body {
  display: flex;
  flex-direction: column;
  gap: var(--e-space-1);
  padding: var(--e-space-4);
  min-width: 0;
}
.product-card__category {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ink);
}
.product-card__name {
  font-size: 1.125rem;
  font-weight: 600;
}
.product-card__description {
  font-size: 0.9375rem;
  line-height: 1.5;
  color: var(--e-color-text-secondary);
}
.product-card__buy {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--e-space-3);
  margin-top: auto;
  padding-top: var(--e-space-3);
}
.product-card__price {
  font-size: 1.25rem;
  font-weight: 700;
  letter-spacing: var(--e-letter-spacing-tight);
}
.product-card__add {
  min-height: var(--shop-target);
}

@media (min-width: 640px) {
  .product-card :deep(.e-card__body) {
    grid-template-columns: 1fr;
    grid-template-rows: auto 1fr;
  }
  .product-card__tile {
    aspect-ratio: 16 / 9;
  }
  .product-card__icon {
    font-size: 3.5rem;
  }
  .product-card__body {
    padding: var(--e-space-5);
  }
  .product-card__buy {
    flex-direction: column;
    align-items: stretch;
  }
  .product-card__add {
    width: 100%;
  }
}
</style>

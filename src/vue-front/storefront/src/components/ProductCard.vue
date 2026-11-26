<script setup lang="ts">
import { computed } from 'vue';
import { categoryLook, productIcon } from '@/category';
import { formatPrice } from '@/format';
import type { Product } from '@/services/CatalogService';

/**
 * One product: a colour block in its category's tint with an icon for what it is, then name, description, price and
 * Add to cart. The parent owns the add; `added` briefly confirms one that just succeeded. `showCategory` is off when
 * the grid is already filtered to one category.
 */
const props = withDefaults(
  defineProps<{ product: Product; adding: boolean; disabled: boolean; added?: boolean; showCategory?: boolean }>(),
  { added: false, showCategory: true },
);
const emit = defineEmits<{ add: [productId: number] }>();

const look = computed(() => categoryLook(props.product.category));
const icon = computed(() => productIcon(props.product.name, props.product.category));
</script>

<template>
  <ECard flush class="product-card" :class="`product-card--${look.key}`">
    <div class="product-card__tile" aria-hidden="true">
      <EIcon :icon="icon" class="product-card__icon" />
    </div>
    <div class="product-card__body">
      <p v-if="showCategory" class="product-card__category">{{ product.category }}</p>
      <h2 class="product-card__name">{{ product.name }}</h2>
      <p v-if="product.description" class="product-card__description">{{ product.description }}</p>
      <div class="product-card__buy">
        <p class="product-card__price num" data-testid="product-price">{{ formatPrice(product.price) }}</p>
        <EButton
          class="product-card__add"
          :label="added ? 'Added' : 'Add to cart'"
          :prepend-icon="added ? 'check' : 'add_shopping_cart'"
          :type="added ? 'success' : 'primary'"
          loading-label="Adding…"
          :aria-label="adding ? undefined : `${added ? 'Added' : 'Add to cart'}: ${product.name}`"
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
.product-card :deep(.e-card__body) {
  display: grid;
  grid-template-columns: 6rem minmax(0, 1fr);
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
  font-size: 2.5rem;
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
/* Price and button always share one row; the button never stretches, so price stays the loudest thing. */
.product-card__buy {
  display: flex;
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
  flex: 0 0 auto;
  min-height: var(--shop-target);
}

/* Narrow phones: the button can't sit beside the price without squeezing it, so it takes its own row. */
@media (max-width: 379.98px) {
  .product-card__buy {
    flex-wrap: wrap;
  }
}

@media (min-width: 640px) {
  .product-card :deep(.e-card__body) {
    grid-template-columns: 1fr;
    grid-template-rows: auto 1fr;
  }
  .product-card__tile {
    height: 7.5rem;
  }
  .product-card__icon {
    font-size: 3.25rem;
  }
  .product-card__body {
    padding: var(--e-space-5);
  }
}
</style>

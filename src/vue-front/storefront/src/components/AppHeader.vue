<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { unreadBadge } from '@guilherme/vue-base';
import { useBasket } from '@/stores/basket';

const { itemCount } = useBasket();
// /orders/:id is a separate route, so RouterLink alone would not mark "My orders" as the current section there.
const route = useRoute();
const inOrders = computed(() => route.path === '/orders' || route.path.startsWith('/orders/'));

const cartLabel = computed(() => (itemCount.value > 0 ? `Cart, ${itemCount.value} items` : 'Cart'));
const badgeText = computed(() => unreadBadge(itemCount.value) as string);
</script>

<template>
  <header class="app-header">
    <div class="app-header__inner">
      <RouterLink to="/" class="app-header__brand">
        <span class="app-header__mark" aria-hidden="true"><EIcon icon="storefront" /></span>
        EShop
      </RouterLink>
      <nav class="app-header__nav" aria-label="Main">
        <RouterLink
          to="/orders"
          class="app-header__link"
          :class="{ 'app-header__link--current': inOrders }"
          aria-label="My orders"
        >
          <EIcon icon="receipt_long" aria-hidden="true" />
          <span class="app-header__text" aria-hidden="true">My orders</span>
        </RouterLink>
        <RouterLink to="/cart" class="app-header__link app-header__cart" :aria-label="cartLabel">
          <span class="app-header__icon">
            <EIcon icon="shopping_cart" aria-hidden="true" />
            <span v-if="itemCount > 0" class="app-header__badge num" data-testid="cart-badge" aria-hidden="true">{{
              badgeText
            }}</span>
          </span>
          <span class="app-header__text" aria-hidden="true">Cart</span>
        </RouterLink>
      </nav>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: var(--e-z-sticky);
  background: var(--e-color-surface);
  border-bottom: 1px solid var(--e-color-border);
}
.app-header__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--e-space-4);
  max-width: calc(var(--shop-content-width) + 2 * var(--shop-gutter));
  height: var(--shop-header-height);
  margin: 0 auto;
  padding: 0 var(--shop-gutter);
}
.app-header__brand {
  display: inline-flex;
  align-items: center;
  gap: var(--e-space-2);
  min-height: var(--shop-target);
  font-family: var(--e-font-family-heading);
  font-size: 1.375rem;
  font-weight: 700;
  letter-spacing: var(--e-letter-spacing-tight);
  color: var(--e-color-text);
  text-decoration: none;
}
.app-header__brand:hover {
  color: var(--e-color-text);
}
.app-header__mark {
  display: inline-grid;
  place-items: center;
  width: 2rem;
  height: 2rem;
  border-radius: var(--e-radius-md);
  background: var(--e-color-primary);
  color: var(--e-color-primary-contrast);
  font-size: 1.25rem;
}
.app-header__nav {
  display: flex;
  align-items: center;
  gap: var(--e-space-2);
}
.app-header__link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--e-space-2);
  min-width: var(--shop-target);
  min-height: var(--shop-target);
  padding: 0 var(--e-space-2);
  border-radius: var(--e-radius-md);
  color: var(--e-color-text);
  font-weight: 600;
  text-decoration: none;
  transition: background-color var(--e-transition-fast), color var(--e-transition-fast);
}
.app-header__link:hover {
  background: var(--e-color-surface-hover);
  color: var(--e-color-text);
}
.app-header__link.router-link-active,
.app-header__link--current {
  color: var(--e-color-primary);
  background: var(--e-color-primary-subtle);
}
.app-header__text {
  display: none;
}
.app-header__icon {
  position: relative;
  display: inline-flex;
}
.app-header__badge {
  position: absolute;
  top: -0.55rem;
  right: -0.7rem;
  min-width: 1.375rem;
  height: 1.375rem;
  padding: 0 0.3rem;
  border: 2px solid var(--e-color-surface);
  border-radius: var(--e-radius-full);
  background: var(--e-color-secondary);
  color: var(--e-color-secondary-contrast);
  font-size: 0.875rem;
  font-weight: 700;
  line-height: 1.1;
  display: inline-grid;
  place-items: center;
}
@media (min-width: 640px) {
  .app-header__nav {
    gap: var(--e-space-3);
  }
  .app-header__link {
    padding: 0 var(--e-space-3);
  }
  .app-header__text {
    display: inline;
  }
  .app-header__cart .app-header__text {
    margin-left: var(--e-space-2);
  }
}
</style>

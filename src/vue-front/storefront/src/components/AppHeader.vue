<script setup lang="ts">
import { computed } from 'vue';
import { unreadBadge } from '@guilherme/vue-base';
import { useBasket } from '@/stores/basket';

const { itemCount } = useBasket();

const cartLabel = computed(() => (itemCount.value > 0 ? `Cart, ${itemCount.value} items` : 'Cart'));
const badgeText = computed(() => unreadBadge(itemCount.value) as string);
</script>

<template>
  <header class="app-header">
    <RouterLink to="/" class="app-header__brand">EShop</RouterLink>
    <nav class="app-header__nav">
      <RouterLink to="/orders">My orders</RouterLink>
      <RouterLink to="/cart" class="app-header__cart" :aria-label="cartLabel">
        <EIcon icon="shopping_cart" />
        <span v-if="itemCount > 0" class="app-header__badge" data-testid="cart-badge">{{ badgeText }}</span>
      </RouterLink>
    </nav>
  </header>
</template>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1.5rem;
  border-bottom: 1px solid var(--e-primitive-slate-200, #dee1e7);
}
.app-header__brand {
  font-weight: 700;
  font-size: 1.25rem;
  text-decoration: none;
  color: inherit;
}
.app-header__nav {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}
.app-header__nav a {
  color: inherit;
  text-decoration: none;
}
.app-header__cart {
  position: relative;
  display: inline-flex;
}
.app-header__badge {
  position: absolute;
  top: -0.4rem;
  right: -0.6rem;
  min-width: 1.1rem;
  height: 1.1rem;
  padding: 0 0.3rem;
  border-radius: 999px;
  background: var(--e-color-danger, #e5484d);
  color: #fff;
  font-size: 0.7rem;
  font-weight: 700;
  line-height: 1.1rem;
  text-align: center;
  box-sizing: border-box;
}
</style>

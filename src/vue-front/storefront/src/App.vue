<script setup lang="ts">
import { onMounted } from 'vue';
import AppHeader from '@/components/AppHeader.vue';
import { refreshBasket } from '@/stores/basket';

// Load the basket once on start, for the header badge. refreshBasket never rejects.
onMounted(() => {
  void refreshBasket();
});
</script>

<template>
  <a class="skip-link" href="#main">Skip to content</a>
  <AppHeader />
  <main id="main" class="app-main" tabindex="-1">
    <RouterView />
  </main>
  <EToast />
</template>

<style scoped>
.app-main {
  max-width: calc(var(--shop-content-width) + 2 * var(--shop-gutter));
  margin: 0 auto;
  padding: var(--e-space-6) var(--shop-gutter) var(--e-space-16);
}
.app-main:focus {
  outline: none;
}
.skip-link {
  position: absolute;
  left: var(--e-space-4);
  top: -100px;
  z-index: var(--e-z-tooltip);
  padding: var(--e-space-2) var(--e-space-4);
  border-radius: var(--e-radius-md);
  background: var(--e-color-primary);
  color: var(--e-color-primary-contrast);
  font-weight: 700;
  text-decoration: none;
}
.skip-link:focus {
  top: var(--e-space-2);
  color: var(--e-color-primary-contrast);
}
@media (min-width: 640px) {
  .app-main {
    padding-top: var(--e-space-8);
  }
}
</style>

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
  <AppHeader />
  <main class="app-main">
    <RouterView />
  </main>
  <EToast />
</template>

<style scoped>
.app-main {
  max-width: 1100px;
  margin: 0 auto;
  padding: 1.5rem 1rem;
}
</style>

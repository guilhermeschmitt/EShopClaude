<script setup lang="ts">
import AppHeader from '@/components/AppHeader.vue';
</script>

<template>
  <AppHeader />
  <main class="app-main">
    <RouterView />
  </main>
</template>

<style scoped>
.app-main {
  max-width: 1100px;
  margin: 0 auto;
  padding: 1.5rem 1rem;
}
</style>

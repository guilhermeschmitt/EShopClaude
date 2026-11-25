<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ProductPager from '@/components/ProductPager.vue';
import { showError } from '@/errors';
import { formatPrice } from '@/format';
import { CatalogService, type Product } from '@/services/CatalogService';

const PAGE_SIZE = 8;

// Created in setup, after Core.init has set Core.baseURL.
const service = new CatalogService();
const route = useRoute();
const router = useRouter();

/** `?page=N` is 1-based; anything but a positive integer means page 1. */
function parsePage(value: unknown): number {
  return typeof value === 'string' && /^[1-9]\d*$/.test(value) ? Number(value) : 1;
}

const page = computed(() => parsePage(route.query.page));
const products = ref<Product[]>([]);
const totalPages = ref(0);
const loading = ref(true);
const failed = ref(false);

// Id of the latest request; an older response that settles later is dropped.
let latest = 0;

async function load(): Promise<void> {
  const id = ++latest;
  loading.value = true;
  failed.value = false;
  products.value = [];
  try {
    const result = await service.listProducts({ page: page.value - 1, size: PAGE_SIZE });
    if (id !== latest) return;
    products.value = result.items;
    totalPages.value = result.totalPages;
  } catch (error) {
    if (id !== latest) return;
    failed.value = true;
    showError('Could not load products', error);
  } finally {
    if (id === latest) loading.value = false;
  }
}

watch(page, () => void load(), { immediate: true });

function onSelect(n: number): void {
  if (n === page.value) {
    // Clicking the current page only retries a failed load.
    if (failed.value) void load();
    return;
  }
  void router.push({ name: 'home', query: n === 1 ? {} : { page: String(n) } });
}
</script>

<template>
  <h1>Products</h1>

  <div v-if="loading" class="product-grid">
    <ECard v-for="n in PAGE_SIZE" :key="n" loading />
  </div>

  <ul v-else-if="products.length" class="product-grid" aria-label="Products">
    <li v-for="product in products" :key="product.id" class="product-card" data-testid="product-card">
      <ECard>
        <h2 class="product-card__name">{{ product.name }}</h2>
        <p class="product-card__category">{{ product.category }}</p>
        <p v-if="product.description" class="product-card__description">{{ product.description }}</p>
        <p class="product-card__price" data-testid="product-price">{{ formatPrice(product.price) }}</p>
      </ECard>
    </li>
  </ul>

  <EEmptyState v-else-if="!failed" icon="inventory_2" title="No products found" />

  <ProductPager
    v-if="totalPages > 1"
    :page="page"
    :total-pages="totalPages"
    :disabled="loading"
    @select="onSelect"
  />
</template>

<style scoped>
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 1rem;
  list-style: none;
  margin: 0;
  padding: 0;
}
.product-card,
.product-card > :deep(.e-card) {
  height: 100%;
}
.product-card__name {
  margin: 0 0 0.25rem;
  font-size: 1.05rem;
  font-weight: 700;
}
.product-card__category {
  margin: 0 0 0.5rem;
  font-size: 0.75rem;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--e-color-text-secondary, #5b6271);
}
.product-card__description {
  margin: 0 0 0.75rem;
  font-size: 0.9rem;
  color: var(--e-color-text-secondary, #5b6271);
}
.product-card__price {
  margin: 0;
  font-weight: 700;
}
</style>

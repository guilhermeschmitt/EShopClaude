<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ProductCard from '@/components/ProductCard.vue';
import ProductPager from '@/components/ProductPager.vue';
import { showError } from '@/errors';
import { CatalogService, type Product } from '@/services/CatalogService';
import { CategoryService } from '@/services/CategoryService';
import { useBasket } from '@/stores/basket';

const PAGE_SIZE = 8;

// Created in setup, after Core.init has set Core.baseURL.
const service = new CatalogService();
const categoryService = new CategoryService();
const route = useRoute();
const router = useRouter();

/** `?page=N` is 1-based; anything but a positive integer means page 1. */
function parsePage(value: unknown): number {
  return typeof value === 'string' && /^[1-9]\d*$/.test(value) ? Number(value) : 1;
}

/** `?category=` / `?q=` are sent to Catalog as typed; missing, blank or repeated means unset (`''`). */
function parseFilter(value: unknown): string {
  return typeof value === 'string' && value.trim() !== '' ? value : '';
}

/** Query for `/`: `category` and `q` when set, `page` only past page 1. */
function homeQuery(c: string, s: string, n: number): Record<string, string> {
  return {
    ...(c ? { category: c } : {}),
    ...(s ? { q: s } : {}),
    ...(n === 1 ? {} : { page: String(n) }),
  };
}

const page = computed(() => parsePage(route.query.page));
const category = computed(() => parseFilter(route.query.category));
const search = computed(() => parseFilter(route.query.q));
const products = ref<Product[]>([]);
const totalPages = ref(0);
const totalItems = ref(0);
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
    const result = await service.listProducts({
      page: page.value - 1,
      size: PAGE_SIZE,
      ...(category.value ? { category: category.value } : {}),
      ...(search.value ? { q: search.value } : {}),
    });
    if (id !== latest) return;
    products.value = result.items;
    totalPages.value = result.totalPages;
    totalItems.value = result.totalItems;
  } catch (error) {
    if (id !== latest) return;
    failed.value = true;
    showError('Could not load products', error);
  } finally {
    if (id === latest) loading.value = false;
  }
}

watch([page, category, search], () => void load(), { immediate: true });

function onSelect(n: number): void {
  if (n === page.value) {
    // Clicking the current page only retries a failed load.
    if (failed.value) void load();
    return;
  }
  void router.push({ name: 'home', query: homeQuery(category.value, search.value, n) });
}

// Categories load once, independently of the products; a failure leaves only "All".
const categories = ref<string[]>([]);
const categoriesLoading = ref(true);
const categoryOptions = computed(() => [
  { label: 'All', value: '' },
  ...categories.value.map((c) => ({ label: c, value: c })),
]);

onMounted(async () => {
  try {
    categories.value = await categoryService.list();
  } catch (error) {
    showError('Could not load categories', error);
  } finally {
    categoriesLoading.value = false;
  }
});

/** A new category resets to page 1; "All" is `''`. */
function onCategory(value: string | null): void {
  const c = value ?? '';
  if (c === category.value) return;
  void router.push({ name: 'home', query: homeQuery(c, search.value, 1) });
}

// The box mirrors the URL's search; typed text only takes effect on submit.
const searchText = ref('');
watch(
  search,
  (s) => {
    searchText.value = s;
  },
  { immediate: true },
);

/** A new search keeps the category and resets to page 1; an unchanged one does nothing. */
function onSearch(): void {
  const s = searchText.value.trim();
  if (s === search.value) return;
  void router.push({ name: 'home', query: homeQuery(category.value, s, 1) });
}

const emptyDescription = computed(() => {
  if (search.value) {
    const where = category.value ? ` in ${category.value}` : '';
    return `Nothing matches "${search.value}"${where}. Try another word or clear the search.`;
  }
  if (category.value) return `There are no products in ${category.value} right now.`;
  return '';
});

const resultCount = computed(() => (totalItems.value === 1 ? '1 product' : `${totalItems.value} products`));

/** Empty-state action: drops the search (keeping the category), or the category when there is no search. */
function onClearFilters(): void {
  void router.push({ name: 'home', query: homeQuery(search.value ? category.value : '', '', 1) });
}

// One add at a time: the clicked card's button loads and every other card's button is disabled until it settles.
const { addItem } = useBasket();
const addingId = ref<number | null>(null);

async function onAdd(productId: number): Promise<void> {
  if (addingId.value !== null) return;
  addingId.value = productId;
  try {
    await addItem(productId);
  } finally {
    addingId.value = null;
  }
}
</script>

<template>
  <div class="page-head">
    <h1>Products</h1>
    <p v-if="!loading && !failed" class="page-head__meta num" role="status" data-testid="product-count">
{{ resultCount }}</p>
  </div>

  <div class="product-filters">
    <div class="product-filters__category">
      <ESelect
        id="category-filter"
        label="Category"
        :options="categoryOptions"
        :model-value="category"
        :loading="categoriesLoading"
        loading-label="Loading…"
        :show-clear="false"
        data-testid="category-filter"
        @update:model-value="onCategory"
      />
    </div>
    <form class="product-search" role="search" aria-label="Product search" @submit.prevent="onSearch">
      <EInputText
        id="product-search"
        v-model="searchText"
        class="product-search__input"
        label="Search"
        type="search"
        autocomplete="off"
        data-testid="product-search"
      />
      <EButton label="Search" native-type="submit" prepend-icon="search" data-testid="product-search-submit" />
    </form>
  </div>

  <ul v-if="loading" class="product-grid" aria-label="Loading products" aria-busy="true">
    <li v-for="n in PAGE_SIZE" :key="n" class="product-grid__item">
      <ECard flush class="product-skeleton">
        <div class="product-skeleton__tile" />
        <div class="product-skeleton__body">
          <ESkeleton height="0.875rem" width="40%" />
          <ESkeleton height="1.125rem" width="80%" />
          <ESkeleton height="0.875rem" width="95%" />
          <ESkeleton height="2.75rem" width="100%" radius="var(--e-radius-md)" />
        </div>
      </ECard>
    </li>
  </ul>

  <ul v-else-if="products.length" class="product-grid" aria-label="Products">
    <li v-for="product in products" :key="product.id" class="product-grid__item" data-testid="product-card">
      <ProductCard
        :product="product"
        :adding="addingId === product.id"
        :disabled="addingId !== null && addingId !== product.id"
        @add="onAdd"
      />
    </li>
  </ul>

  <ECard v-else-if="!failed">
    <EEmptyState icon="search_off" title="No products found" :description="emptyDescription">
      <EButton
        v-if="search || category"
        :label="search ? 'Clear search' : 'Show all products'"
        outlined
        data-testid="clear-filters"
        @click="onClearFilters"
      />
    </EEmptyState>
  </ECard>

  <ProductPager
    v-if="totalPages > 1"
    :page="page"
    :total-pages="totalPages"
    :disabled="loading"
    @select="onSelect"
  />
</template>

<style scoped>
.product-filters {
  display: grid;
  gap: var(--e-space-3);
  margin-bottom: var(--e-space-6);
}
.product-search {
  display: flex;
  align-items: stretch;
  gap: var(--e-space-2);
}
.product-search__input {
  flex: 1;
  min-width: 0;
}
.product-search > :deep(.e-button) {
  min-height: var(--shop-target);
  align-self: stretch;
}
.product-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--e-space-4);
  list-style: none;
  margin: 0;
  padding: 0;
}
.product-grid__item {
  min-width: 0;
}
.product-skeleton :deep(.e-card__body) {
  display: grid;
  grid-template-columns: 6.5rem minmax(0, 1fr);
  gap: 0;
  height: 100%;
}
.product-skeleton__tile {
  min-height: 9rem;
  background: var(--e-color-skeleton);
}
.product-skeleton__body {
  display: flex;
  flex-direction: column;
  gap: var(--e-space-3);
  padding: var(--e-space-4);
}

@media (min-width: 640px) {
  .product-filters {
    grid-template-columns: 16rem minmax(0, 28rem);
    align-items: end;
  }
  .product-grid {
    grid-template-columns: repeat(auto-fill, minmax(15rem, 1fr));
    gap: var(--e-space-5);
  }
  .product-skeleton :deep(.e-card__body) {
    grid-template-columns: 1fr;
  }
  .product-skeleton__tile {
    aspect-ratio: 16 / 9;
    min-height: 0;
  }
  .product-skeleton__body {
    padding: var(--e-space-5);
  }
}

@media (min-width: 1200px) {
  .product-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
</style>

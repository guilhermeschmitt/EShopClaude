<script setup lang="ts">
import { computed, ref } from 'vue';
import { formatPrice } from '@/format';
import { useBasket } from '@/stores/basket';

// Reads the shared basket (App.vue loads it on start) and never GETs it itself. +/- and remove send a PUT whose
// response becomes the shared basket, so the lines, total and header badge update from that one request.
const { basket, status, updateQuantity } = useBasket();
const items = computed(() => basket.value?.items ?? []);
const total = computed(() => basket.value?.total ?? 0);

// One adjust at a time: while a PUT is in flight every +, - and remove button is disabled.
const updatingId = ref<number | null>(null);

async function onAdjust(productId: number, quantity: number): Promise<void> {
  if (updatingId.value !== null) return;
  updatingId.value = productId;
  try {
    await updateQuantity(productId, quantity);
  } finally {
    updatingId.value = null;
  }
}
</script>

<template>
  <h1>Cart</h1>

  <ECard v-if="status === 'loading'" loading />

  <template v-else-if="status === 'ready' && items.length === 0">
    <EEmptyState icon="shopping_cart" title="Your cart is empty" />
    <RouterLink to="/" class="cart-browse">Browse products</RouterLink>
  </template>

  <ECard v-else-if="status === 'ready'">
    <table class="cart-table" aria-label="Cart lines">
      <thead>
        <tr>
          <th scope="col">Product</th>
          <th scope="col" class="cart-table__num">Unit price</th>
          <th scope="col" class="cart-table__num">Quantity</th>
          <th scope="col" class="cart-table__num">Line total</th>
          <th scope="col"><span class="visually-hidden">Actions</span></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.productId" data-testid="cart-line">
          <td>{{ item.productName }}</td>
          <td class="cart-table__num">{{ formatPrice(item.unitPrice) }}</td>
          <td class="cart-table__num">
            <div class="cart-qty">
              <EButton
                icon="remove"
                size="small"
                outlined
                :label="`Decrease quantity of ${item.productName}`"
                :disabled="updatingId !== null"
                data-testid="qty-decrease"
                @click="onAdjust(item.productId, item.quantity - 1)"
              />
              <span class="cart-qty__value" data-testid="line-quantity">{{ item.quantity }}</span>
              <EButton
                icon="add"
                size="small"
                outlined
                :label="`Increase quantity of ${item.productName}`"
                :disabled="updatingId !== null"
                data-testid="qty-increase"
                @click="onAdjust(item.productId, item.quantity + 1)"
              />
            </div>
          </td>
          <td class="cart-table__num" data-testid="line-total">{{ formatPrice(item.unitPrice * item.quantity) }}</td>
          <td>
            <EButton
              icon="delete"
              size="small"
              type="danger"
              ghost
              :label="`Remove ${item.productName}`"
              :disabled="updatingId !== null"
              data-testid="line-remove"
              @click="onAdjust(item.productId, 0)"
            />
          </td>
        </tr>
      </tbody>
      <tfoot>
        <tr>
          <th scope="row" colspan="3">Total</th>
          <td class="cart-table__num" data-testid="cart-total">{{ formatPrice(total) }}</td>
          <td></td>
        </tr>
      </tfoot>
    </table>
  </ECard>
</template>

<style scoped>
.cart-table {
  width: 100%;
  border-collapse: collapse;
}
.cart-table th,
.cart-table td {
  padding: 0.5rem 0.75rem;
  text-align: left;
  vertical-align: middle;
}
.cart-table tr {
  border-bottom: 1px solid var(--e-primitive-slate-200, #dee1e7);
}
.cart-table tfoot tr {
  border-bottom: none;
}
.cart-table .cart-table__num {
  text-align: right;
}
.cart-table tfoot {
  font-weight: 700;
}
.cart-qty {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}
.cart-qty__value {
  min-width: 2ch;
  text-align: center;
}
.cart-browse {
  display: inline-block;
  margin-top: 0.75rem;
}
.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
  border: 0;
}
</style>

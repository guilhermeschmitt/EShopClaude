<script setup lang="ts">
import { formatPrice } from '@/format';

/** One basket or order line, as Basket and Ordering return it. */
export interface Line {
  productId: number;
  productName: string;
  unitPrice: number;
  quantity: number;
}

/**
 * Product lines as a table from 640px and as stacked line cards below it (the table's own rows re-flow, so there is
 * one DOM and one set of test ids at every width). `editable` adds the -/+ and remove controls used by the cart.
 */
const props = withDefaults(
  defineProps<{
    items: readonly Line[];
    label: string;
    lineTestid: string;
    editable?: boolean;
    disabled?: boolean;
  }>(),
  { editable: false, disabled: false },
);
const emit = defineEmits<{ adjust: [productId: number, quantity: number] }>();

function adjust(line: Line, quantity: number): void {
  if (!props.disabled) emit('adjust', line.productId, quantity);
}
</script>

<template>
  <ECard flush class="line-items">
    <table class="line-items__table" role="table" :aria-label="label" :class="{ 'line-items__table--editable': editable }">
      <thead role="rowgroup">
        <tr role="row">
          <th scope="col" role="columnheader">Product</th>
          <th scope="col" role="columnheader" class="line-items__num">Unit price</th>
          <th scope="col" role="columnheader" class="line-items__num">Quantity</th>
          <th scope="col" role="columnheader" class="line-items__num">Line total</th>
          <th v-if="editable" scope="col" role="columnheader"><span class="visually-hidden">Actions</span></th>
        </tr>
      </thead>
      <tbody role="rowgroup">
        <tr v-for="(item, i) in items" :key="`${item.productId}-${i}`" role="row" :data-testid="lineTestid">
          <th scope="row" role="rowheader" class="line-items__name">{{ item.productName }}</th>
          <td role="cell" class="line-items__num line-items__unit" data-label="Unit price">
            <span class="num" data-testid="line-unit-price">{{ formatPrice(item.unitPrice) }}</span>
          </td>
          <td role="cell" class="line-items__num line-items__qty" data-label="Quantity">
            <div v-if="editable" class="line-items__stepper">
              <EButton
                icon="remove"
                size="small"
                outlined
                :label="`Decrease quantity of ${item.productName}`"
                :disabled="disabled"
                data-testid="qty-decrease"
                @click="adjust(item, item.quantity - 1)"
              />
              <span class="line-items__qty-value num" data-testid="line-quantity">{{ item.quantity }}</span>
              <EButton
                icon="add"
                size="small"
                outlined
                :label="`Increase quantity of ${item.productName}`"
                :disabled="disabled"
                data-testid="qty-increase"
                @click="adjust(item, item.quantity + 1)"
              />
            </div>
            <span v-else class="num" data-testid="line-quantity">{{ item.quantity }}</span>
          </td>
          <td role="cell" class="line-items__num line-items__total" data-label="Line total">
            <span class="num" data-testid="line-total">{{ formatPrice(item.unitPrice * item.quantity) }}</span>
          </td>
          <td v-if="editable" role="cell" class="line-items__remove">
            <EButton
              icon="delete"
              size="small"
              type="danger"
              ghost
              :label="`Remove ${item.productName}`"
              :disabled="disabled"
              data-testid="line-remove"
              @click="adjust(item, 0)"
            />
          </td>
        </tr>
      </tbody>
    </table>
  </ECard>
</template>

<style scoped>
.line-items__table {
  width: 100%;
  border-collapse: collapse;
}
.line-items__table th,
.line-items__table td {
  padding: var(--e-space-4);
  text-align: left;
  vertical-align: middle;
}
.line-items__table thead th {
  padding-block: var(--e-space-3);
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--e-color-text-secondary);
  background: var(--e-color-surface-hover);
  white-space: nowrap;
}
.line-items__table thead th:first-child {
  border-top-left-radius: calc(var(--e-radius-lg) - 1px);
}
.line-items__table thead th:last-child {
  border-top-right-radius: calc(var(--e-radius-lg) - 1px);
}
.line-items__table tbody tr + tr {
  border-top: 1px solid var(--e-color-border);
}
.line-items__name {
  font-family: var(--e-font-family-heading);
  font-size: 1.0625rem;
  font-weight: 600;
  overflow-wrap: anywhere;
}
.line-items__table .line-items__num {
  text-align: right;
  white-space: nowrap;
}
.line-items__unit,
.line-items__qty {
  font-size: 1rem;
  color: var(--e-color-text);
}
.line-items__total {
  font-size: 1.0625rem;
  font-weight: 600;
}
.line-items__stepper {
  display: inline-flex;
  align-items: center;
  gap: var(--e-space-2);
}
.line-items__stepper :deep(.e-button),
.line-items__remove :deep(.e-button) {
  width: var(--shop-target);
  height: var(--shop-target);
}
.line-items__qty-value {
  min-width: 2.5ch;
  text-align: center;
  font-size: 1.0625rem;
  font-weight: 600;
}
.line-items__remove {
  width: 1%;
  padding-left: 0;
}
/* Fixed number columns, so cart, checkout and order line up the same way. */
.line-items__table .line-items__unit {
  width: 8.5rem;
}
.line-items__table .line-items__qty {
  width: 11rem;
}
.line-items__table .line-items__total {
  width: 8.5rem;
}

/* Phones: each row becomes a line card. Headers stay for screen readers; cells show their own labels. */
@media (max-width: 639.98px) {
  .line-items__table thead {
    position: absolute;
    width: 1px;
    height: 1px;
    overflow: hidden;
    clip-path: inset(50%);
  }
  .line-items__table tbody tr {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    grid-template-areas:
      'name name'
      'unit total'
      'qty qty';
    column-gap: var(--e-space-4);
    row-gap: var(--e-space-3);
    padding: var(--e-space-4);
  }
  .line-items__table--editable tbody tr {
    grid-template-areas:
      'name remove'
      'unit total'
      'qty qty';
  }
  .line-items__table tbody th,
  .line-items__table tbody td {
    padding: 0;
  }
  .line-items__name {
    grid-area: name;
  }
  .line-items__unit {
    grid-area: unit;
  }
  .line-items__total {
    grid-area: total;
  }
  .line-items__qty {
    grid-area: qty;
  }
  .line-items__remove {
    grid-area: remove;
    width: auto;
    margin: calc(-1 * var(--e-space-2)) calc(-1 * var(--e-space-2)) 0 0;
  }
  .line-items__table .line-items__unit,
  .line-items__table .line-items__qty,
  .line-items__table .line-items__total {
    width: auto;
  }
  .line-items__table .line-items__unit {
    text-align: left;
  }
  .line-items__table .line-items__qty {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-top: var(--e-space-3);
    border-top: 1px dashed var(--e-color-border);
  }
  .line-items__unit,
  .line-items__total {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .line-items__total {
    align-items: flex-end;
  }
  .line-items__table td[data-label]::before {
    content: attr(data-label);
    font-family: var(--e-font-family-base);
    font-size: 0.875rem;
    font-weight: 600;
    color: var(--e-color-text-secondary);
  }
  .line-items__total {
    font-size: 1.125rem;
  }
}
</style>

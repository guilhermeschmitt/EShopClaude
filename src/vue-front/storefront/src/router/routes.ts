import type { RouteRecordRaw } from 'vue-router';
import HomeView from '@/views/HomeView.vue';
import CartView from '@/views/CartView.vue';
import CheckoutView from '@/views/CheckoutView.vue';
import OrdersView from '@/views/OrdersView.vue';
import OrderDetailView from '@/views/OrderDetailView.vue';

// Every route is public: vue-base's guard only blocks routes with meta.permission, so none is set here.
export const routes: RouteRecordRaw[] = [
  { path: '/', name: 'home', component: HomeView, meta: { title: 'Products' } },
  { path: '/cart', name: 'cart', component: CartView, meta: { title: 'Cart' } },
  { path: '/checkout', name: 'checkout', component: CheckoutView, meta: { title: 'Checkout' } },
  { path: '/orders', name: 'orders', component: OrdersView, meta: { title: 'My orders' } },
  { path: '/orders/:id', name: 'order-detail', component: OrderDetailView, meta: { title: 'Order' } },
];

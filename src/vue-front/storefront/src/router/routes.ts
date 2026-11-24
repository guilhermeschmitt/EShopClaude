import type { RouteRecordRaw } from 'vue-router';
import HomeView from '@/views/HomeView.vue';
import CartView from '@/views/CartView.vue';
import CheckoutView from '@/views/CheckoutView.vue';
import OrdersView from '@/views/OrdersView.vue';
import OrderDetailView from '@/views/OrderDetailView.vue';

// Every route is public: vue-base's guard only blocks routes with meta.permission, so none is set here.
export const routes: RouteRecordRaw[] = [
  { path: '/', name: 'home', component: HomeView },
  { path: '/cart', name: 'cart', component: CartView },
  { path: '/checkout', name: 'checkout', component: CheckoutView },
  { path: '/orders', name: 'orders', component: OrdersView },
  { path: '/orders/:id', name: 'order-detail', component: OrderDetailView },
];

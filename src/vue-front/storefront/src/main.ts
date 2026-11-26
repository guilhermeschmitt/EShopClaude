import { createApp } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';
import { Core } from '@guilherme/vue-base';
import '@guilherme/vue-base/style.css';
import '@guilherme/vue-base/icons.css';
import '@fontsource-variable/rubik';
import '@fontsource-variable/nunito-sans';
// The theme comes after vue-base's stylesheet so its tokens win at equal specificity.
import './styles/tokens.css';
import './styles/base.css';
import App from './App.vue';
import { routes } from './router/routes';
import { getBuyerId } from './buyer';

// Create the anonymous buyerId before anything renders, whatever route is opened first.
getBuyerId();

const app = createApp(App);
const router = createRouter({ history: createWebHistory(), routes });

// Registers vue-base's components globally and installs its route guard (no auth on these routes).
await Core.init(app, router, routes, { baseURL: '/api' });

// Each page names itself in the tab: "Cart – EShop"; an order page includes its number.
router.afterEach((to) => {
  const title = to.meta.title as string | undefined;
  const page = to.name === 'order-detail' ? `Order #${String(to.params.id)}` : title;
  document.title = page ? `${page} – EShop` : 'EShop';
});

app.use(router);
app.mount('#app');

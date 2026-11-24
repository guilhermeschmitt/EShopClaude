import { createApp } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';
import { Core } from '@guilherme/vue-base';
import '@guilherme/vue-base/style.css';
import '@guilherme/vue-base/icons.css';
import App from './App.vue';
import { routes } from './router/routes';
import { getBuyerId } from './buyer';

// Create the anonymous buyerId before anything renders, whatever route is opened first.
getBuyerId();

const app = createApp(App);
const router = createRouter({ history: createWebHistory(), routes });

// Registers vue-base's components globally and installs its route guard (no auth on these routes).
await Core.init(app, router, routes, { baseURL: '/api' });

app.use(router);
app.mount('#app');

import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

// Aspire injects services__<name>__http__0 for each service the storefront references. Each /api/<name> entry
// exists only when that URL is set (not in `npm run build` or a bare `npm run dev`), and strips its prefix.
// No server.port: Aspire assigns the port.
const basketUrl = process.env.services__basket__http__0;

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: basketUrl
      ? {
          '/api/basket': {
            target: basketUrl,
            changeOrigin: true,
            rewrite: (path) => path.replace(/^\/api\/basket/, ''),
          },
        }
      : {},
  },
});

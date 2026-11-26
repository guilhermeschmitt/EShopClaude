import { fileURLToPath, URL } from 'node:url';
import { defineConfig, type ProxyOptions } from 'vite';
import vue from '@vitejs/plugin-vue';

// Aspire injects services__<name>__http__0 for each service the storefront references. Each /api/<name> entry
// exists only when that URL is set (not in `npm run build` or a bare `npm run dev`), and strips its prefix.
// No server.port: Aspire assigns the port.
const serviceUrls: Record<string, string | undefined> = {
  basket: process.env.services__basket__http__0,
  catalog: process.env.services__catalog__http__0,
  ordering: process.env.services__ordering__http__0,
};

// Aspire's endpoint proxy keeps accepting connections for a stopped service and never answers. Without a timeout the
// request hangs and the error never reaches the UI; with it, Vite answers 502 and the error toast shows.
const PROXY_TIMEOUT_MS = 10_000;

/** One `/api/<name>` proxy entry per service whose URL is set. */
function proxyEntries(urls: Record<string, string | undefined>): Record<string, ProxyOptions> {
  const entries: Record<string, ProxyOptions> = {};
  for (const [name, target] of Object.entries(urls)) {
    if (!target) {
      continue;
    }
    const prefix = new RegExp(`^/api/${name}`);
    entries[`/api/${name}`] = {
      target,
      changeOrigin: true,
      proxyTimeout: PROXY_TIMEOUT_MS,
      rewrite: (path) => path.replace(prefix, ''),
    };
  }
  return entries;
}

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: proxyEntries(serviceUrls),
  },
});

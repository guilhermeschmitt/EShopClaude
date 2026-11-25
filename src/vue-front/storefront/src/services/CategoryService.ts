import { ApiService, Core } from '@guilherme/vue-base';

/**
 * Catalog's categories through the storefront proxy: `/api/catalog` (proxy prefix) + `/categories` (resource).
 * `GET /categories` returns the distinct categories, sorted.
 * Construct only after `Core.init`, because `config()` reads `Core.baseURL`.
 */
export class CategoryService extends ApiService {
  config(): { baseURL: string; resource: string } {
    return { baseURL: `${Core.baseURL as string}/catalog`, resource: 'categories' };
  }

  async list(): Promise<string[]> {
    const response: { data: string[] } = await this.request.get('');
    return response.data;
  }
}

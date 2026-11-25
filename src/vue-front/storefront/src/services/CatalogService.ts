import { ApiService, Core } from '@guilherme/vue-base';

/** Catalog's Product. `price` is for display only; Basket takes its own snapshot from Catalog on add. */
export interface Product {
  id: number;
  name: string;
  description: string | null;
  price: number;
  category: string;
}

/** `GET /products`: one zero-based page of products, ordered by name. */
export interface ProductPage {
  items: Product[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

/**
 * Catalog API through the storefront proxy: `/api/catalog` (proxy prefix) + `/products` (resource).
 * Construct only after `Core.init`, because `config()` reads `Core.baseURL`.
 */
export class CatalogService extends ApiService {
  config(): { baseURL: string; resource: string } {
    return { baseURL: `${Core.baseURL as string}/catalog`, resource: 'products' };
  }

  async listProducts(params: { page: number; size: number; category?: string }): Promise<ProductPage> {
    const response: { data: ProductPage } = await this.request.get('', { params });
    return response.data;
  }
}

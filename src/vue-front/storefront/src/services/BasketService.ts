import { ApiService, Core } from '@guilherme/vue-base';

/** A basket line: the name and price are the snapshot Basket took from Catalog when the item was added. */
export interface BasketItem {
  productId: number;
  productName: string;
  unitPrice: number;
  quantity: number;
}

/** `GET /basket/{buyerId}`. A buyer with no basket gets `items: []` and `total: 0`. */
export interface Basket {
  buyerId: string;
  items: BasketItem[];
  total: number;
}

/** `POST /basket/{buyerId}/checkout` → 201: the id of the order Ordering created. */
export interface CheckoutResult {
  orderId: number;
}

/**
 * Basket API through the storefront proxy: `/api/basket` (proxy prefix) + `/basket` (resource).
 * Construct only after `Core.init`, because `config()` reads `Core.baseURL`.
 */
export class BasketService extends ApiService {
  config(): { baseURL: string; resource: string } {
    return { baseURL: `${Core.baseURL as string}/basket`, resource: 'basket' };
  }

  async get(buyerId: string): Promise<Basket> {
    const response: { data: Basket } = await this.request.get(buyerId);
    return response.data;
  }

  /** `POST /basket/{buyerId}/items`: one unit of the product. Basket looks the price up; none is sent. */
  async addItem(buyerId: string, productId: number): Promise<Basket> {
    const response: { data: Basket } = await this.request.post(`${buyerId}/items`, { productId, quantity: 1 });
    return response.data;
  }

  /** `PUT /basket/{buyerId}/items/{productId}`: sets the line's quantity, and `0` removes the line. Only the quantity
   * is sent. 404 when the basket has no such line. */
  async updateQuantity(buyerId: string, productId: number, quantity: number): Promise<Basket> {
    const response: { data: Basket } = await this.request.put(`${buyerId}/items/${productId}`, { quantity });
    return response.data;
  }

  /** `POST /basket/{buyerId}/checkout`: no body is sent. Basket creates the order through Ordering with its snapshot
   * prices and clears the basket only after the order exists. Returns the new order's id; 400 when the basket is empty. */
  async checkout(buyerId: string): Promise<number> {
    const response: { data: CheckoutResult } = await this.request.post(`${buyerId}/checkout`);
    return response.data.orderId;
  }
}

import { ApiService, Core } from '@guilherme/vue-base';

/** `GET /orders?buyerId=`: one order without its lines. `itemCount` is the sum of the line quantities. */
export interface OrderSummary {
  id: number;
  /** ISO-8601 UTC, e.g. `2026-11-26T15:04:05.123456Z`. */
  createdAt: string;
  total: number;
  itemCount: number;
}

/**
 * Ordering API through the storefront proxy: `/api/ordering` (proxy prefix) + `/orders` (resource).
 * Construct only after `Core.init`, because `config()` reads `Core.baseURL`.
 */
export class OrderingService extends ApiService {
  config(): { baseURL: string; resource: string } {
    return { baseURL: `${Core.baseURL as string}/ordering`, resource: 'orders' };
  }

  /** `GET /orders?buyerId={buyerId}`: the buyer's orders, newest first, not paged. `[]` for a buyer with none. */
  async listByBuyer(buyerId: string): Promise<OrderSummary[]> {
    const response: { data: OrderSummary[] } = await this.request.get('', { params: { buyerId } });
    return response.data;
  }
}

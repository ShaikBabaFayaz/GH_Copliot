export type Product = {
  id: string;
  name: string;
  sku: string;
  description: string | null;
  category: string;
  unitPrice: string;
  minimumThreshold: string;
  currentStock: string;
  active: boolean;
  version: number;
};

export type ProductDraft = Pick<Product,
  'name' | 'sku' | 'description' | 'category' | 'unitPrice' | 'minimumThreshold'>;

export type StockMovement = {
  id: number;
  productId: string;
  type: 'IN' | 'OUT';
  quantity: string;
  reasonReference: string;
  actorUsername: string;
  occurredAt: string;
};

export type ProductFilters = {
  query: string;
  category: string;
  stockStatus: '' | 'IN_STOCK' | 'LOW_STOCK';
};

export class ApiError extends Error {
  constructor(readonly status: number, message: string) {
    super(message);
    this.name = 'ApiError';
  }
}

export class InventoryApi {
  private readonly authorization: string;

  constructor(username: string, password: string, private readonly onUnauthorized: () => void) {
    this.authorization = `Basic ${btoa(`${username}:${password}`)}`;
  }

  async authenticate() {
    return this.request<{ username: string; role: string }>('/auth/me');
  }

  async listProducts(filters?: ProductFilters) {
    const params = new URLSearchParams();
    if (filters?.query.trim()) params.set('query', filters.query.trim());
    if (filters?.category) params.set('category', filters.category);
    if (filters?.stockStatus) params.set('stockStatus', filters.stockStatus);
    const query = params.size ? `?${params}` : '';
    const products = await this.request<Product[]>(`/products${query}`);
    return products.map(normalizeProduct);
  }

  async listCategories() {
    return this.request<string[]>('/categories');
  }

  async listInactiveProducts() {
    return (await this.request<Product[]>('/products/inactive')).map(normalizeProduct);
  }

  async getInactiveProduct(id: string) {
    return normalizeProduct(await this.request<Product>(`/products/inactive/${encodeURIComponent(id)}`));
  }

  async listLowStockProducts() {
    const products = await this.request<Product[]>('/products/low-stock');
    return products.map(normalizeProduct);
  }

  async getProduct(id: string) {
    return normalizeProduct(await this.request<Product>(`/products/${encodeURIComponent(id)}`));
  }

  async saveProduct(draft: ProductDraft, id?: string) {
    const product = await this.request<Product>(id ? `/products/${encodeURIComponent(id)}` : '/products', {
      method: id ? 'PUT' : 'POST',
      body: JSON.stringify(draft),
    });
    return normalizeProduct(product);
  }

  async deactivateProduct(id: string) {
    return normalizeProduct(await this.request<Product>(`/products/${encodeURIComponent(id)}/deactivation`, {
      method: 'PATCH',
    }));
  }

  async listMovements(id: string) {
    return this.request<StockMovement[]>(`/products/${encodeURIComponent(id)}/movements`);
  }

  async recordMovement(id: string, movement: {
    type: 'IN' | 'OUT';
    quantity: string;
    reasonReference: string;
    expectedVersion: number;
  }) {
    return this.request(`/products/${encodeURIComponent(id)}/movements`, {
      method: 'POST',
      body: JSON.stringify(movement),
    });
  }

  private async request<T>(path: string, init: RequestInit = {}): Promise<T> {
    let response: Response;
    try {
      response = await fetch(`/api/v1${path}`, {
        ...init,
        headers: {
          Accept: 'application/json, application/problem+json',
          Authorization: this.authorization,
          ...(init.body ? { 'Content-Type': 'application/json' } : {}),
          ...init.headers,
        },
      });
    } catch {
      throw new ApiError(0, 'The service could not be reached. Check the connection and try again.');
    }

    if (response.status === 401) this.onUnauthorized();
    if (!response.ok) {
      const problem = await response.json().catch(() => null) as { detail?: string } | null;
      throw new ApiError(response.status, problem?.detail || 'The request could not be completed.');
    }
    if (response.status === 204) return undefined as T;
    return response.json() as Promise<T>;
  }
}

function normalizeProduct(product: Product): Product {
  return {
    ...product,
    description: product.description ?? null,
    unitPrice: String(product.unitPrice),
    minimumThreshold: String(product.minimumThreshold),
    currentStock: String(product.currentStock),
  };
}
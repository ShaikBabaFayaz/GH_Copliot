import { useEffect, useState } from 'react';
import type { FormEvent, ReactNode } from 'react';
import { Link, Navigate, Route, Routes, useLocation, useNavigate, useParams } from 'react-router';

import { ApiError, InventoryApi } from './api';
import type { Product, ProductDraft, ProductFilters, StockMovement } from './api';

function App() {
  const [api, setApi] = useState<InventoryApi | null>(null);
  const [managerName, setManagerName] = useState('');
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<string[]>([]);
  const [movements, setMovements] = useState<StockMovement[]>([]);
  const [movementRecovery, setMovementRecovery] = useState<{
    productId: string;
    phase: 'refresh-required' | 'review-required';
  } | null>(null);
  const [filters, setFilters] = useState<ProductFilters>({ query: '', category: '', stockStatus: '' });
  const [loading, setLoading] = useState(false);
  const [pageError, setPageError] = useState('');
  const [loginError, setLoginError] = useState('');
  const [loginPending, setLoginPending] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();

  async function signIn(username: string, password: string) {
    setLoginPending(true);
    setLoginError('');
    const candidate = new InventoryApi(username, password, () => {
      setApi(null);
      setProducts([]);
      setCategories([]);
      setMovements([]);
      setMovementRecovery(null);
      setLoginError('Your session ended. Sign in again to continue.');
    });
    try {
      const identity = await candidate.authenticate();
      const [productList, categoryList] = await Promise.all([
        candidate.listProducts(),
        candidate.listCategories(),
      ]);
      setManagerName(identity.username);
      setProducts(productList);
      setCategories(categoryList);
      setApi(candidate);
    } catch (error) {
      setLoginError(error instanceof ApiError && error.status === 403
        ? 'Access is denied for this account.'
        : error instanceof ApiError && error.status === 0
          ? error.message
          : 'Username or password was not accepted.');
    } finally {
      setLoginPending(false);
    }
  }

  function signOut() {
    setApi(null);
    setManagerName('');
    setProducts([]);
    setCategories([]);
    setMovements([]);
    setMovementRecovery(null);
    setPageError('');
    navigate('/products');
  }

  useEffect(() => {
    if (!api) return;
    const client = api;
    const path = location.pathname;
    let cancelled = false;
    setLoading(true);
    setPageError('');

    async function loadPage() {
      try {
        if (path === '/products') {
          const result = await client.listProducts(filters);
          if (cancelled) return;
          setProducts(result);
          setMovements([]);
        } else if (path === '/inactive-products') {
          const result = await client.listInactiveProducts();
          if (cancelled) return;
          setProducts(result);
          setMovements([]);
        } else if (path === '/low-stock') {
          const result = await client.listLowStockProducts();
          if (cancelled) return;
          setProducts(result);
          setMovements([]);
        } else {
          const match = path.match(/^\/products\/([^/]+)(?:\/(inactive|edit|stock))?$/);
          const id = match?.[1];
          const isInactiveRoute = match?.[2] === 'inactive';
          if (id && id !== 'new') {
            const productPromise = isInactiveRoute ? client.getInactiveProduct(id) : client.getProduct(id);
            const movementPromise = client.listMovements(id);
            const [product, history] = await Promise.all([productPromise, movementPromise]);
            if (cancelled) return;
            setProducts((current) => [...current.filter((item) => item.id !== id), product]);
            setMovements(history);
          }
        }
      } catch (error) {
        if (!cancelled) setPageError(apiErrorMessage(error));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    void loadPage();
    return () => { cancelled = true; };
  }, [api, filters, location.pathname]);

  async function saveProduct(draft: ProductDraft, id?: string) {
    if (!api) return;
    setPageError('');
    try {
      await api.saveProduct(draft, id);
      navigate(id ? `/products/${id}` : '/products');
    } catch (error) {
      setPageError(apiErrorMessage(error));
      throw error;
    }
  }

  async function deactivateProduct(id: string) {
    if (!api) return;
    setPageError('');
    try {
      await api.deactivateProduct(id);
      navigate('/products');
    } catch (error) {
      setPageError(apiErrorMessage(error));
    }
  }

  async function recordMovement(id: string, type: StockMovement['type'], quantity: string, reasonReference: string) {
    if (!api) return;
    const product = products.find((item) => item.id === id);
    if (!product) throw new Error('Product details are unavailable. Reload before recording stock.');
    setPageError('');
    try {
      await api.recordMovement(id, { type, quantity, reasonReference, expectedVersion: product.version });
      setMovementRecovery(null);
      navigate(`/products/${id}`);
    } catch (error) {
      if (error instanceof ApiError && (error.status === 0 || error.status === 409 || error.status >= 500)) {
        setMovementRecovery({ productId: id, phase: 'refresh-required' });
        try {
          const [currentProduct, history] = await Promise.all([api.getProduct(id), api.listMovements(id)]);
          setProducts((current) => [...current.filter((item) => item.id !== id), currentProduct]);
          setMovements(history);
          setMovementRecovery({ productId: id, phase: 'review-required' });
        } catch {
          setPageError('Could not confirm the latest stock balance. Reload the product before any retry.');
        }
      }
      throw error;
    }
  }

  async function refreshStock(id: string) {
    if (!api) return;
    setPageError('');
    try {
      const [currentProduct, history] = await Promise.all([api.getProduct(id), api.listMovements(id)]);
      setProducts((current) => [...current.filter((item) => item.id !== id), currentProduct]);
      setMovements(history);
      setMovementRecovery({ productId: id, phase: 'review-required' });
    } catch (error) {
      setPageError(`Could not confirm the latest stock balance. ${apiErrorMessage(error)}`);
      setMovementRecovery({ productId: id, phase: 'refresh-required' });
    }
  }

  const stockRouteId = location.pathname.match(/^\/products\/([^/]+)\/stock$/)?.[1];

  if (!api) return <LoginPage onLogin={signIn} error={loginError} pending={loginPending} />;

  return (
    <div className="workspace">
      <header className="topbar">
        <Link className="wordmark" to="/products" aria-label="Inventory home">Inventory</Link>
        <div className="topbar-actions">
          <span className="user-label">{managerName}</span>
          <button className="button button-quiet" type="button" onClick={signOut}>Sign out</button>
        </div>
      </header>
      <div className="workspace-body">
        <aside className="sidebar" aria-label="Main navigation">
          <span className="nav-caption">WORKSPACE</span>
          <Link className={`nav-link ${location.pathname === '/products' ? 'nav-link-active' : ''}`} to="/products">Products</Link>
          <Link className={`nav-link ${location.pathname.startsWith('/inactive-products') ? 'nav-link-active' : ''}`} to="/inactive-products">Inactive products</Link>
          <Link className={`nav-link ${location.pathname === '/low-stock' ? 'nav-link-active' : ''}`} to="/low-stock">Low stock</Link>
          <span className="sidebar-note">Single location</span>
          <div className="sidebar-footnote">Local inventory data is cleared when the service restarts.</div>
        </aside>
        <main className="main-content">
          {pageError && <p className="form-error global-error" role="alert">{pageError}</p>}
          <Routes>
            <Route path="/products" element={<ProductListPage products={products} categories={categories} loading={loading}
              filters={filters} onApplyFilters={setFilters} />} />
            <Route path="/inactive-products" element={<InactiveProductListPage products={products} loading={loading} />} />
            <Route path="/products/new" element={<ProductFormPage categories={categories} onSave={saveProduct} />} />
            <Route path="/products/:id/edit" element={<ExistingProductForm products={products} categories={categories} onSave={saveProduct} />} />
            <Route path="/low-stock" element={<LowStockPage products={products} loading={loading} />} />
            <Route path="/products/:id/stock" element={<StockMovementPage products={products}
              movements={movements} recoveryPhase={movementRecovery?.productId === stockRouteId
                ? movementRecovery?.phase ?? null : null} onRecord={recordMovement} onRefresh={refreshStock}
              onConfirmReview={() => setMovementRecovery(null)} />} />
            <Route path="/products/:id/inactive" element={<ProductDetailPage products={products} movements={movements}
              loading={loading} onDeactivate={deactivateProduct} />} />
            <Route path="/products/:id" element={<ProductDetailPage products={products} movements={movements}
              loading={loading} onDeactivate={deactivateProduct} />} />
            <Route path="*" element={<Navigate to="/products" replace />} />
          </Routes>
        </main>
      </div>
    </div>
  );
}

function LoginPage({ onLogin, error: serverError, pending }: {
  onLogin: (username: string, password: string) => Promise<void>;
  error: string;
  pending: boolean;
}) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!username.trim() || !password) {
      setError('Enter your username and password.');
      return;
    }
    void onLogin(username.trim(), password);
  }

  return (
    <main className="login-page">
      <section className="login-panel" aria-labelledby="login-title">
        <div className="login-mark" aria-hidden="true">I</div>
        <p className="eyebrow">INVENTORY MANAGEMENT</p>
        <h1 id="login-title">Sign in</h1>
        <p className="login-copy">Manage products and stock for your location.</p>
        <form className="form-stack" onSubmit={submit} noValidate>
          <Field label="Username">
            <input autoComplete="username" value={username} onChange={(event) => setUsername(event.target.value)} />
          </Field>
          <Field label="Password">
            <input type="password" autoComplete="current-password" value={password}
              onChange={(event) => setPassword(event.target.value)} />
          </Field>
          {(error || serverError) && <p className="form-error" role="alert">{error || serverError}</p>}
          <button className="button button-primary button-wide" type="submit" disabled={pending}>
            {pending ? 'Signing in…' : 'Continue'}
          </button>
        </form>
        <p className="login-security">Access is limited to inventory managers.</p>
      </section>
    </main>
  );
}

function ProductListPage({ products, categories, loading, filters, onApplyFilters }: {
  products: Product[];
  categories: string[];
  loading: boolean;
  filters: ProductFilters;
  onApplyFilters: (filters: ProductFilters) => void;
}) {
  const { query, category, stockStatus } = filters;
  const navigate = useNavigate();
  const filteredProducts = products;
  const hasFilters = Boolean(query || category || stockStatus);

  function clearFilters() {
    onApplyFilters({ query: '', category: '', stockStatus: '' });
  }

  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">CATALOG</p>
          <h1>Products</h1>
          <p className="page-subtitle">{filteredProducts.length} {hasFilters ? 'matching' : 'active'} {filteredProducts.length === 1 ? 'product' : 'products'}</p>
        </div>
        <button className="button button-primary" type="button" onClick={() => navigate('/products/new')}>
          Add product
        </button>
        <button className="button button-secondary" type="button" onClick={() => navigate('/inactive-products')}>
          Inactive products
        </button>
      </div>
      <section className="filter-bar" aria-label="Search and filter products">
        <label className="search-field">
          <span className="sr-only">Search name or SKU</span>
          <span className="search-symbol" aria-hidden="true">⌕</span>
          <input placeholder="Search name or SKU" value={query} onChange={(event) => onApplyFilters({ ...filters, query: event.target.value })} />
        </label>
        <label className="filter-field">
          <span className="sr-only">Category</span>
          <select value={category} onChange={(event) => onApplyFilters({ ...filters, category: event.target.value })}>
            <option value="">All categories</option>
            {categories.map((option) => <option key={option} value={option}>{option}</option>)}
          </select>
        </label>
        <label className="filter-field">
          <span className="sr-only">Stock status</span>
          <select value={stockStatus} onChange={(event) => onApplyFilters({ ...filters, stockStatus: event.target.value as ProductFilters['stockStatus'] })}>
            <option value="">Any stock status</option>
            <option value="IN_STOCK">In stock</option>
            <option value="LOW_STOCK">Low stock</option>
          </select>
        </label>
        {hasFilters && <button className="button button-quiet clear-button" type="button" onClick={clearFilters}>Clear</button>}
      </section>
      <section className="table-region" aria-label="Product results">
        <div className="table-caption"><span>Inventory</span><span>{filteredProducts.length} results</span></div>
        {filteredProducts.length > 0 ? (
          <div className="table-scroll">
            <table>
              <thead>
                <tr><th>Product</th><th>Category</th><th>Unit price</th><th>Stock</th><th>Status</th></tr>
              </thead>
              <tbody>
                {filteredProducts.map((product) => {
                  const low = Number(product.currentStock) <= Number(product.minimumThreshold);
                  return (
                    <tr key={product.id} className="product-row" onClick={() => navigate(`/products/${product.id}`)}>
                      <td><Link className="product-name" to={`/products/${product.id}`} onClick={(event) => event.stopPropagation()}>{product.name}</Link>
                        <span className="sku">{product.sku}</span></td>
                      <td>{product.category}</td>
                      <td>{formatInr(product.unitPrice)}</td>
                      <td>{formatCount(product.currentStock)} <span className="muted">/ {formatCount(product.minimumThreshold)} min</span></td>
                      <td><StatusTag tone={low ? 'warning' : 'success'}>{low ? 'Low stock' : 'In stock'}</StatusTag></td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="empty-state">
            <span className="empty-mark" aria-hidden="true">—</span>
            <h2>{loading ? 'Loading products' : hasFilters ? 'No matching products' : 'No products yet'}</h2>
            <p>{loading ? 'Inventory is being loaded.' : hasFilters
              ? 'Try a different search or clear the selected filters.'
              : 'Add a product to start building your inventory.'}</p>
            {!loading && !hasFilters
              ? <button className="button button-primary" type="button" onClick={() => navigate('/products/new')}>Add first product</button>
              : null}
            {!loading && hasFilters
              ? <button className="button button-secondary" type="button" onClick={clearFilters}>Clear filters</button>
              : null}
          </div>
        )}
      </section>
    </>
  );
}

function ExistingProductForm({ products, categories, onSave }: {
  products: Product[];
  categories: string[];
  onSave: (draft: ProductDraft, id?: string) => Promise<void>;
}) {
  const { id = '' } = useParams();
  const product = products.find((candidate) => candidate.id === id);
  if (!product) return <NotFound />;
  if (!product.active) return <Navigate to={`/products/${id}`} replace />;
  return <ProductFormPage initial={product} categories={categories} onSave={onSave} />;
}

function ProductFormPage({ initial, categories, onSave }: {
  initial?: Product;
  categories: string[];
  onSave: (draft: ProductDraft, id?: string) => Promise<void>;
}) {
  const [name, setName] = useState(initial?.name ?? '');
  const [sku, setSku] = useState(initial?.sku ?? '');
  const [category, setCategory] = useState(initial?.category ?? '');
  const [unitPrice, setUnitPrice] = useState(initial?.unitPrice ?? '');
  const [minimumThreshold, setMinimumThreshold] = useState(initial?.minimumThreshold ?? '');
  const [description, setDescription] = useState(initial?.description ?? '');
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);
  const navigate = useNavigate();

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalizedName = name.trim();
    const normalizedSku = sku.trim().toUpperCase();
    const normalizedCategory = category.trim();
    const normalizedPrice = unitPrice.trim();
    const normalizedThreshold = minimumThreshold.trim();
    const normalizedDescription = description.trim();
    const priceParts = normalizedPrice.split('.');
    const priceInMinorUnits = priceParts.length <= 2 && /^\d+$/.test(priceParts[0] ?? '')
      && /^\d*$/.test(priceParts[1] ?? '')
      ? BigInt(priceParts[0] ?? '0') * 100n + BigInt((priceParts[1] ?? '').padEnd(2, '0') || '0')
      : -1n;
    const priceLimitInMinorUnits = 99999999999999n;

    if (Array.from(normalizedName).length < 1 || Array.from(normalizedName).length > 120) {
      setError('Name must contain 1 to 120 characters.');
    } else if (!/^[A-Z0-9][A-Z0-9._/-]{0,63}$/.test(normalizedSku)) {
      setError('Enter a valid SKU using letters, numbers, dot, underscore, slash or hyphen.');
    } else if (!normalizedCategory) {
      setError('Choose a category.');
    } else if (!/^\d+(?:\.\d{1,2})?$/.test(normalizedPrice)
      || priceInMinorUnits < 0n || priceInMinorUnits > priceLimitInMinorUnits) {
      setError('Enter a non-negative INR price with no more than 2 decimal places.');
    } else if (!/^\d{1,12}$/.test(normalizedThreshold)) {
      setError('Enter a whole-number minimum threshold from 0 to 999999999999.');
    } else if (Array.from(normalizedDescription).length > 2000) {
      setError('Description must not exceed 2,000 characters.');
    } else if (containsDisallowedRequiredTextControl(normalizedName)
      || containsDisallowedRequiredTextControl(normalizedSku)
      || containsDisallowedRequiredTextControl(normalizedCategory)
      || /[\u0000-\u0008\u000B-\u001F\u007F-\u009F]/.test(normalizedDescription)) {
      setError('Text contains a control character that is not allowed.');
    } else {
      const [whole, fraction = ''] = normalizedPrice.split('.');
      setPending(true);
      setError('');
      try {
        await onSave({
          name: normalizedName,
          sku: normalizedSku,
          category: normalizedCategory,
          unitPrice: `${whole}.${fraction.padEnd(2, '0')}`,
          minimumThreshold: normalizedThreshold,
          description: normalizedDescription || null,
        }, initial?.id);
      } catch (saveError) {
        setError(apiErrorMessage(saveError));
      } finally {
        setPending(false);
      }
    }
  }

  return (
    <>
      <div className="page-heading page-heading-narrow">
        <div><p className="eyebrow">PRODUCTS</p><h1>{initial ? 'Edit product' : 'Add product'}</h1>
          <p className="page-subtitle">Product information and stock threshold.</p></div>
      </div>
      <form className="form-surface" onSubmit={submit} noValidate>
        <p id="required-fields-note" className="field-hint">Fields marked with * are required.</p>
        <div className="form-grid">
          <Field label="Product name" required>
            <textarea required aria-describedby={error ? 'product-form-error' : 'required-fields-note'} rows={1} value={name} onChange={(event) => setName(event.target.value)} maxLength={240} />
          </Field>
          <Field label="SKU" required hint="Letters, numbers, . _ / -">
            <input required aria-describedby={error ? 'product-form-error' : 'required-fields-note'} value={sku} onChange={(event) => setSku(event.target.value)} autoCapitalize="characters" maxLength={64} />
          </Field>
          <Field label="Category" required hint="Choose an available category.">
            <select required aria-describedby={error ? 'product-form-error' : 'required-fields-note'} value={category} onChange={(event) => setCategory(event.target.value)}>
              <option value="">Choose category</option>
              {categories.map((option) => <option key={option} value={option}>{option}</option>)}
            </select>
          </Field>
          <Field label="Unit price (INR)" required>
            <div className="input-prefix"><span aria-hidden="true">₹</span><input required aria-describedby={error ? 'product-form-error' : 'required-fields-note'} inputMode="decimal" value={unitPrice}
              onChange={(event) => setUnitPrice(event.target.value)} placeholder="0.00" /></div>
          </Field>
          <Field label="Minimum stock threshold" required hint="Whole countable units only.">
            <input required aria-describedby={error ? 'product-form-error' : 'required-fields-note'} inputMode="numeric" value={minimumThreshold} onChange={(event) => setMinimumThreshold(event.target.value)} />
          </Field>
          <Field label="Description" hint="Optional, up to 2,000 characters." className="field-full">
            <textarea rows={4} value={description} onChange={(event) => setDescription(event.target.value)} />
          </Field>
        </div>
        {error && <p id="product-form-error" className="form-error" role="alert">{error}</p>}
        <div className="form-actions">
          <button className="button button-secondary" type="button" onClick={() => navigate(initial ? `/products/${initial.id}` : '/products')}>Cancel</button>
          <button className="button button-primary" type="submit" disabled={pending}>
            {pending ? 'Saving…' : initial ? 'Save changes' : 'Create product'}
          </button>
        </div>
      </form>
    </>
  );
}

function LowStockPage({ products, loading }: { products: Product[]; loading: boolean }) {
  const lowStockProducts = products.filter((product) => product.active
    && BigInt(product.currentStock) <= BigInt(product.minimumThreshold));

  return (
    <>
      <div className="page-heading">
        <div><p className="eyebrow">MONITORING</p><h1>Low stock</h1>
          <p className="page-subtitle">{lowStockProducts.length} products at or below their minimum threshold</p></div>
      </div>
      <section className="table-region" aria-label="Low-stock products">
        <div className="table-caption"><span>Replenishment watch</span><span>{lowStockProducts.length} results</span></div>
        {lowStockProducts.length ? (
          <div className="table-scroll"><table>
            <thead><tr><th>Product</th><th>Category</th><th>Current stock</th><th>Minimum</th><th>Unit price</th></tr></thead>
            <tbody>{lowStockProducts.map((product) => (
              <tr key={product.id}>
                <td><Link className="product-name" to={`/products/${product.id}`}>{product.name}</Link><span className="sku">{product.sku}</span></td>
                <td>{product.category}</td><td>{formatCount(product.currentStock)}</td>
                <td>{formatCount(product.minimumThreshold)}</td><td>{formatInr(product.unitPrice)}</td>
              </tr>
            ))}</tbody>
          </table></div>
        ) : <div className="empty-state"><span className="empty-mark" aria-hidden="true">{loading ? '…' : '✓'}</span>
          <h2>{loading ? 'Loading low-stock products' : 'All active products are above threshold'}</h2>
          <p>{loading ? 'Checking current balances.' : 'No replenishment attention is needed.'}</p>
          {!loading && <Link className="button button-secondary" to="/products">View products</Link>}</div>}
      </section>
    </>
  );
}

function StockMovementPage({ products, movements, recoveryPhase, onRecord, onRefresh, onConfirmReview }: {
  products: Product[];
  movements: StockMovement[];
  recoveryPhase: 'refresh-required' | 'review-required' | null;
  onRecord: (id: string, type: StockMovement['type'], quantity: string, reason: string) => void;
  onRefresh: (id: string) => Promise<void>;
  onConfirmReview: () => void;
}) {
  const { id = '' } = useParams();
  const [type, setType] = useState<StockMovement['type']>('IN');
  const [quantity, setQuantity] = useState('');
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);
  const { product, missing } = findProduct(products, id);
  if (missing) return <NotFound />;
  if (!product || !product.active) return <Navigate to={`/products/${id}`} replace />;
  const currentStock = BigInt(product.currentStock);
  const nextStock = quantity && /^\d+$/.test(quantity)
    ? currentStock + (type === 'IN' ? BigInt(quantity) : -BigInt(quantity))
    : currentStock;

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!/^[1-9]\d{0,11}$/.test(quantity)) {
      setError('Quantity must be a positive whole number up to 999999999999.');
    } else if (!reason.trim()) {
      setError('Enter a reason or reference for this movement.');
    } else if (type === 'OUT' && BigInt(quantity) > currentStock) {
      setError('Quantity cannot exceed current stock.');
    } else if (type === 'IN' && currentStock + BigInt(quantity) > 999999999999n) {
      setError('Resulting stock cannot exceed 999999999999.');
    } else {
      setPending(true);
      setError('');
      try {
        await onRecord(id, type, quantity, reason);
      } catch (movementError) {
        setError(apiErrorMessage(movementError));
      } finally {
        setPending(false);
      }
    }
  }

  return (
    <>
      <div className="breadcrumb"><Link to={`/products/${id}`}>{product.name}</Link><span aria-hidden="true">/</span><span>Stock movement</span></div>
      <div className="page-heading page-heading-narrow"><div><p className="eyebrow">STOCK CONTROL</p>
        <h1>Record movement</h1><p className="page-subtitle">{product.sku} · current balance {formatCount(product.currentStock)}</p></div></div>
      <form className="form-surface" onSubmit={submit} noValidate>
        <p id="movement-required-fields-note" className="field-hint">Fields marked with * are required.</p>
        <div className="movement-mode" role="group" aria-label="Movement direction">
          <button className={`mode-option ${type === 'IN' ? 'mode-selected' : ''}`} type="button" aria-pressed={type === 'IN'} onClick={() => setType('IN')}>IN · Receive</button>
          <button className={`mode-option ${type === 'OUT' ? 'mode-selected' : ''}`} type="button" aria-pressed={type === 'OUT'} onClick={() => setType('OUT')}>OUT · Issue</button>
        </div>
        <div className="form-grid movement-fields">
          <Field label="Quantity" required hint="Positive whole units only.">
            <input required aria-describedby={error ? 'movement-form-error' : 'movement-required-fields-note'} inputMode="numeric" value={quantity} onChange={(event) => setQuantity(event.target.value)} />
          </Field>
          <Field label="Expected balance after movement" hint="Preview only; verify before submitting.">
            <input readOnly value={formatCount(nextStock.toString())} />
          </Field>
          <Field label="Expected product version">
            <input readOnly value={product.version.toString()} />
          </Field>
          <Field label="Reason or reference" required className="field-full">
            <textarea required aria-describedby={error ? 'movement-form-error' : 'movement-required-fields-note'} rows={3} value={reason} onChange={(event) => setReason(event.target.value)} maxLength={2000} />
          </Field>
        </div>
        {error && <p id="movement-form-error" className="form-error" role="alert">{error}</p>}
        {recoveryPhase === 'refresh-required' && <div role="alert" className="form-error">
          <p>Stock outcome is not confirmed. Refresh the balance and history before retrying.</p>
          <button className="button button-secondary" type="button" onClick={() => void onRefresh(id)}>Refresh balance and history</button>
        </div>}
        {recoveryPhase === 'review-required' && <div role="status" className="field-hint">
          <p>Balance and history are current. Review them before submitting another movement.</p>
          <button className="button button-secondary" type="button" onClick={onConfirmReview}>I reviewed the current stock</button>
        </div>}
        <div className="form-actions">
          <Link className="button button-secondary" to={`/products/${id}`}>Cancel</Link>
          <button className="button button-primary" type="submit" disabled={pending || recoveryPhase !== null}>
            {pending ? 'Recording…' : `Record ${type === 'IN' ? 'receipt' : 'issue'}`}
          </button>
        </div>
      </form>
      {recoveryPhase && <section className="history-section" aria-labelledby="recovery-history-title">
        <div className="section-heading"><div><p className="eyebrow">AUDIT TRAIL</p><h2 id="recovery-history-title">Latest movement history</h2></div></div>
        <MovementHistory movements={movements.filter((movement) => movement.productId === id)} />
      </section>}
    </>
  );
}

function ProductDetailPage({ products, movements, loading, onDeactivate }: {
  products: Product[];
  movements: StockMovement[];
  loading: boolean;
  onDeactivate: (id: string) => Promise<void>;
}) {
  const { id = '' } = useParams();
  const [confirming, setConfirming] = useState(false);
  const navigate = useNavigate();
  const product = products.find((candidate) => candidate.id === id);
  if (!product && loading) return <p className="loading-state" role="status">Loading product…</p>;
  if (!product) return <NotFound />;
  const low = Number(product.currentStock) <= Number(product.minimumThreshold);

  return (
    <>
      <div className="breadcrumb"><Link to="/products">Products</Link><span aria-hidden="true">/</span><span>{product.name}</span></div>
      <div className="page-heading detail-heading">
        <div><p className="eyebrow">PRODUCT DETAIL</p><h1>{product.name}</h1>
          <p className="page-subtitle">{product.sku} <span className="dot-separator">·</span> {product.category}</p></div>
        <div className="heading-actions">
          {product.active && <button className="button button-secondary" type="button" onClick={() => navigate(`/products/${id}/edit`)}>Edit product</button>}
          <StatusTag tone={product.active ? 'success' : 'neutral'}>{product.active ? 'Active' : 'Inactive'}</StatusTag>
        </div>
      </div>
      <section className="detail-grid" aria-label="Product details">
        <DetailItem label="Unit price" value={formatInr(product.unitPrice)} />
        <DetailItem label="Current stock" value={formatCount(product.currentStock)} />
        <DetailItem label="Minimum threshold" value={formatCount(product.minimumThreshold)} />
        <DetailItem label="Stock status" value={low ? 'Low stock' : 'In stock'} />
      </section>
      <section className="history-section" aria-labelledby="history-title">
        <div className="section-heading"><div><p className="eyebrow">AUDIT TRAIL</p><h2 id="history-title">Movement history</h2></div>
          {product.active && <button className="button button-primary" type="button" onClick={() => navigate(`/products/${id}/stock`)}>Record movement</button>}
        </div>
        <MovementHistory movements={movements.filter((movement) => movement.productId === id)} />
      </section>
      {product.description && <section className="description-section"><h2>Description</h2><p>{product.description}</p></section>}
      {product.active && <section className="danger-zone">
        <div><h2>Deactivate product</h2><p>The product will leave active inventory. Its ID and movement history will be retained.</p></div>
        {!confirming
          ? <button className="button button-danger" type="button" onClick={() => setConfirming(true)}>Deactivate</button>
          : <div className="confirm-actions"><button className="button button-secondary" type="button" onClick={() => setConfirming(false)}>Keep active</button>
            <button className="button button-danger" type="button" onClick={() => onDeactivate(product.id)}>Confirm deactivation</button></div>}
      </section>}
    </>
  );
}

function InactiveProductListPage({ products, loading }: { products: Product[]; loading: boolean }) {
  const inactiveProducts = products.filter((product) => !product.active);
  return <>
    <div className="page-heading">
      <div><p className="eyebrow">ARCHIVE</p><h1>Inactive products</h1>
        <p className="page-subtitle">{inactiveProducts.length} retained products</p></div>
      <Link className="button button-secondary" to="/products">Back to products</Link>
    </div>
    <section className="table-region" aria-label="Inactive product results">
      <div className="table-caption"><span>Retained records</span><span>{inactiveProducts.length} results</span></div>
      {inactiveProducts.length ? <div className="table-scroll"><table>
        <thead><tr><th>Product</th><th>Category</th><th>Unit price</th><th>Stock</th><th>Status</th></tr></thead>
        <tbody>{inactiveProducts.map((item) => <tr key={item.id}>
          <td><Link className="product-name" to={`/products/${item.id}/inactive`}>{item.name}</Link><span className="sku">{item.sku}</span></td>
          <td>{item.category}</td><td>{formatInr(item.unitPrice)}</td><td>{formatCount(item.currentStock)}</td>
          <td><StatusTag tone="neutral">Inactive</StatusTag></td>
        </tr>)}</tbody>
      </table></div> : <div className="empty-state">
        <h2>{loading ? 'Loading inactive products' : 'No inactive products'}</h2>
        <p>{loading ? 'Retained records are being loaded.' : 'Deactivated products will appear here with their history.'}</p>
      </div>}
    </section>
  </>;
}

function MovementHistory({ movements }: { movements: StockMovement[] }) {
  if (!movements.length) {
    return <div className="history-empty">No stock movements have been recorded.</div>;
  }
  return <div className="table-region history-table"><div className="table-scroll"><table>
    <thead><tr><th>Direction</th><th>Quantity</th><th>Reason / reference</th><th>Recorded by</th><th>Date and time</th></tr></thead>
    <tbody>{movements.map((movement) => (
      <tr key={movement.id}><td><StatusTag tone={movement.type === 'IN' ? 'success' : 'neutral'}>{movement.type}</StatusTag></td>
        <td>{formatCount(movement.quantity)}</td><td>{movement.reasonReference}</td><td>{movement.actorUsername}</td>
        <td>{new Date(movement.occurredAt).toLocaleString()}</td></tr>
    ))}</tbody>
  </table></div></div>;
}

function findProduct(products: Product[], id: string) {
  const product = products.find((candidate) => candidate.id === id);
  return { product, missing: !product };
}

function Field({ label, required, hint, className = '', children }: {
  label: string;
  required?: boolean;
  hint?: string;
  className?: string;
  children: ReactNode;
}) {
  return <label className={`field ${className}`}>
    <span className="field-label">{label}{required && <span className="required-mark"> *</span>}</span>
    {children}
    {hint && <span className="field-hint">{hint}</span>}
  </label>;
}

function DetailItem({ label, value }: { label: string; value: string }) {
  return <div className="detail-item"><dt>{label}</dt><dd>{value}</dd></div>;
}

function StatusTag({ tone, children }: { tone: 'success' | 'warning' | 'neutral'; children: string }) {
  return <span className={`status-tag status-${tone}`}><span aria-hidden="true" className="status-dot" />{children}</span>;
}

function NotFound() {
  return <section className="not-found"><h1>Product not found</h1><p>This product may have been removed from the current view.</p><Link className="button button-secondary" to="/products">Back to products</Link></section>;
}

function formatInr(value: string) {
  const [whole, fraction = ''] = value.split('.');
  const formattedWhole = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(BigInt(whole || '0'));
  return `${formattedWhole}.${fraction.padEnd(2, '0')}`;
}

function formatCount(value: string) {
  return new Intl.NumberFormat('en-IN', { maximumFractionDigits: 0 }).format(Number(value));
}

function apiErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 401) return 'Your session ended. Sign in again to continue.';
    if (error.status === 403) return 'Access is denied for this account.';
    return error.message;
  }
  return error instanceof Error ? error.message : 'The request could not be completed.';
}

export function containsDisallowedRequiredTextControl(value: string) {
  return /[\u0000-\u0008\u000B-\u001F\u007F-\u009F]/.test(value);
}

export default App;
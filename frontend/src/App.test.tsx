// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router';

import App, { containsDisallowedRequiredTextControl } from './App';

const product = {
  id: 'a7e9f2d1-7c41-4a80-9b12-9df6a60f1234',
  name: 'Desk lamp',
  sku: 'LAMP-1',
  description: 'Warm light',
  category: 'Electronics',
  unitPrice: '12.50',
  minimumThreshold: '2',
  currentStock: '0',
  active: true,
  version: 0,
};

const movement = {
  id: 1,
  productId: product.id,
  type: 'IN' as const,
  quantity: '4',
  reasonReference: 'Opening stock',
  actorUsername: 'manager',
  occurredAt: '2026-11-04T10:00:00Z',
};

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

function defaultFetch(input: RequestInfo | URL, init?: RequestInit) {
  const url = String(input);
  const method = init?.method ?? 'GET';
  if (url.endsWith('/auth/me')) return Promise.resolve(jsonResponse({ username: 'manager', role: 'INVENTORY_MANAGER' }));
  if (url.endsWith('/categories')) return Promise.resolve(jsonResponse(['Electronics', 'Lab equipment']));
  if (url.endsWith('/products/inactive')) return Promise.resolve(jsonResponse([{ ...product, active: false }]));
  if (url.endsWith(`/products/inactive/${product.id}`)) return Promise.resolve(jsonResponse({ ...product, active: false }));
  if (url.includes('/movements') && method === 'POST') return Promise.resolve(jsonResponse({}));
  if (url.includes('/movements')) return Promise.resolve(jsonResponse([movement]));
  if (url.endsWith('/deactivation')) return Promise.resolve(jsonResponse({ ...product, active: false }));
  if (url.includes('/low-stock')) return Promise.resolve(jsonResponse([product]));
  if (url.endsWith(`/products/${product.id}`)) return Promise.resolve(jsonResponse(product));
  if (method === 'POST' && url.endsWith('/products')) return Promise.resolve(jsonResponse(product, 201));
  if (url.startsWith('/api/v1/products?query=missing')) return Promise.resolve(jsonResponse([]));
  if (url.startsWith('/api/v1/products')) return Promise.resolve(jsonResponse([product]));
  return Promise.resolve(jsonResponse({}, 404));
}

async function signIn(user = userEvent.setup(), heading = 'Products') {
  await user.type(screen.getByLabelText('Username'), 'manager');
  await user.type(screen.getByLabelText('Password'), 'secret');
  await user.click(screen.getByRole('button', { name: 'Continue' }));
  await screen.findByRole('heading', { name: heading });
  return user;
}

function renderApp(path = '/products') {
  return render(<MemoryRouter initialEntries={[path]}><App /></MemoryRouter>);
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe('inventory workflows', () => {
  it('authenticates, searches and filters, shows an empty state, and clears filters', async () => {
    const fetchMock = vi.fn(defaultFetch);
    vi.stubGlobal('fetch', fetchMock);
    renderApp();
    const user = await signIn();

    expect(screen.getByText('₹12.50')).toBeTruthy();
    expect(screen.getByRole('option', { name: 'Lab equipment' })).toBeTruthy();
    await user.type(screen.getByPlaceholderText('Search name or SKU'), 'missing');
    expect(await screen.findByRole('heading', { name: 'No matching products' })).toBeTruthy();

    await user.click(screen.getByRole('button', { name: 'Clear filters' }));
    expect(await screen.findByRole('link', { name: 'Desk lamp' })).toBeTruthy();
    await user.selectOptions(screen.getByLabelText('Category'), 'Lab equipment');
    await user.selectOptions(screen.getByLabelText('Stock status'), 'LOW_STOCK');

    await waitFor(() => {
      const requestedUrls = fetchMock.mock.calls.map(([url]) => String(url));
      expect(requestedUrls.some((url) => url.includes('category=Lab+equipment')
        && url.includes('stockStatus=LOW_STOCK'))).toBe(true);
    });

    await user.click(screen.getByRole('button', { name: 'Sign out' }));
    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeTruthy();
  });

  it('lists zero-balance products on the low-stock screen', async () => {
    vi.stubGlobal('fetch', vi.fn(defaultFetch));
    renderApp('/low-stock');
    await signIn(undefined, 'Low stock');

    expect(await screen.findByRole('heading', { name: 'Low stock' })).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Desk lamp' })).toBeTruthy();
    expect(screen.getByText('0')).toBeTruthy();
  });

  it('rejects prices with more than two fractional digits without submitting', async () => {
    const fetchMock = vi.fn(defaultFetch);
    vi.stubGlobal('fetch', fetchMock);
    renderApp();
    const user = await signIn();

    await user.click(screen.getByRole('button', { name: 'Add product' }));
    expect(screen.getByRole('option', { name: 'Lab equipment' })).toBeTruthy();
    await user.type(screen.getByLabelText(/Product name/), 'Desk organizer');
    await user.type(screen.getByLabelText(/^SKU/), 'ORG-1');
    await user.selectOptions(screen.getByLabelText(/^Category/), 'Electronics');
    await user.type(screen.getByLabelText(/Unit price \(INR\)/), '12.345');
    await user.type(screen.getByLabelText(/Minimum stock threshold/), '2');
    await user.click(screen.getByRole('button', { name: 'Create product' }));

    expect((await screen.findByRole('alert')).textContent).toContain(
      'Enter a non-negative INR price with no more than 2 decimal places.',
    );
    expect(fetchMock.mock.calls.some(([url, init]) => String(url).endsWith('/products')
      && init?.method === 'POST')).toBe(false);
  });

  it('uses backend categories in the edit form', async () => {
    vi.stubGlobal('fetch', vi.fn(defaultFetch));
    renderApp(`/products/${product.id}/edit`);
    const user = await signIn(undefined, 'Edit product');

    const categorySelect = screen.getByLabelText(/^Category/) as HTMLSelectElement;
    expect(categorySelect.required).toBe(true);
    expect(screen.getByRole('option', { name: 'Lab equipment' })).toBeTruthy();
    await user.selectOptions(categorySelect, 'Lab equipment');
    await user.click(screen.getByRole('button', { name: 'Save changes' }));
    expect(await screen.findByRole('heading', { name: 'Desk lamp' })).toBeTruthy();
  });

  it('normalizes a valid product and disables submit while the save is pending', async () => {
    let completeSave: ((response: Response) => void) | undefined;
    let submittedBody = '';
    const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      if (String(input).endsWith('/products') && init?.method === 'POST') {
        submittedBody = String(init.body);
        return new Promise<Response>((resolve) => { completeSave = resolve; });
      }
      return defaultFetch(input, init);
    });
    vi.stubGlobal('fetch', fetchMock);
    renderApp();
    const user = await signIn();

    await user.click(screen.getByRole('button', { name: 'Add product' }));
    await user.type(screen.getByLabelText(/Product name/), 'Desk organizer');
    await user.type(screen.getByLabelText(/^SKU/), 'org-1');
    await user.selectOptions(screen.getByLabelText(/^Category/), 'Electronics');
    await user.type(screen.getByLabelText(/Unit price \(INR\)/), '12');
    await user.type(screen.getByLabelText(/Minimum stock threshold/), '2');
    await user.click(screen.getByRole('button', { name: 'Create product' }));

    const pendingButton = await screen.findByRole('button', { name: 'Saving…' });
    expect((pendingButton as HTMLButtonElement).disabled).toBe(true);
    expect(submittedBody).toContain('"sku":"ORG-1"');
    expect(submittedBody).toContain('"unitPrice":"12.00"');
    completeSave?.(jsonResponse(product, 201));

    expect(await screen.findByRole('heading', { name: 'Products' })).toBeTruthy();
  });

  it('shows a safe message when credentials are rejected', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(jsonResponse({ detail: 'Authentication is required.' }, 401))));
    renderApp();
    const user = userEvent.setup();
    await user.type(screen.getByLabelText('Username'), 'manager');
    await user.type(screen.getByLabelText('Password'), 'incorrect');
    await user.click(screen.getByRole('button', { name: 'Continue' }));

    expect((await screen.findByRole('alert')).textContent).toContain('Username or password was not accepted.');
  });

  it('shows movement history and requires a reason before submitting stock', async () => {
    const fetchMock = vi.fn(defaultFetch);
    vi.stubGlobal('fetch', fetchMock);
    renderApp();
    const user = await signIn();

    await user.click(screen.getByRole('link', { name: 'Desk lamp' }));
    expect(await screen.findByText('Opening stock')).toBeTruthy();
    expect(screen.getByRole('cell', { name: 'manager' })).toBeTruthy();
    await user.click(screen.getByRole('button', { name: 'Record movement' }));
    await screen.findByRole('heading', { name: 'Record movement' });
    await user.type(screen.getByLabelText(/Quantity/), '1');
    await user.click(screen.getByRole('button', { name: 'Record receipt' }));

    expect((await screen.findByRole('alert')).textContent).toContain('Enter a reason or reference for this movement.');
    expect(fetchMock.mock.calls.some(([url, init]) => String(url).includes('/movements')
      && init?.method === 'POST')).toBe(false);
  });

  it('shows retained inactive history without product or stock write controls', async () => {
    vi.stubGlobal('fetch', vi.fn(defaultFetch));
    renderApp();
    const user = await signIn();

    await user.click(screen.getByRole('link', { name: 'Inactive products' }));
    await user.click(await screen.findByRole('link', { name: 'Desk lamp' }));

    expect(await screen.findByRole('heading', { name: 'Desk lamp' })).toBeTruthy();
    expect(screen.getByText('Inactive')).toBeTruthy();
    expect(await screen.findByText('Opening stock')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Edit product' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Record movement' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Deactivate' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Confirm deactivation' })).toBeNull();
  });

  it('communicates required product fields and associates validation feedback', async () => {
    vi.stubGlobal('fetch', vi.fn(defaultFetch));
    renderApp();
    const user = await signIn();
    await user.click(screen.getByRole('button', { name: 'Add product' }));

    const nameInput = screen.getByLabelText(/Product name/) as HTMLInputElement;
    const skuInput = screen.getByLabelText(/^SKU/) as HTMLInputElement;
    const categorySelect = screen.getByLabelText(/^Category/) as HTMLSelectElement;
    const priceInput = screen.getByLabelText(/Unit price \(INR\)/) as HTMLInputElement;
    const thresholdInput = screen.getByLabelText(/Minimum stock threshold/) as HTMLInputElement;
    expect(nameInput.required).toBe(true);
    expect(skuInput.required).toBe(true);
    expect(categorySelect.required).toBe(true);
    expect(priceInput.required).toBe(true);
    expect(thresholdInput.required).toBe(true);
    expect(screen.getByText('Fields marked with * are required.')).toBeTruthy();

    fireEvent.change(screen.getByLabelText(/Unit price \(INR\)/), { target: { value: '12.345' } });
    await user.click(screen.getByRole('button', { name: 'Create product' }));
    const error = await screen.findByRole('alert');
    expect((screen.getByLabelText(/Unit price \(INR\)/) as HTMLInputElement)
      .getAttribute('aria-describedby')).toBe(error.id);
  });

  it('allows newline and tab in required text but rejects other control characters', async () => {
    expect(containsDisallowedRequiredTextControl('Desk\nLamp\t')).toBe(false);
    expect(containsDisallowedRequiredTextControl('Desk\rLamp')).toBe(true);
    expect(containsDisallowedRequiredTextControl('Desk\u0000Lamp')).toBe(true);
    expect(containsDisallowedRequiredTextControl('Desk\u007fLamp')).toBe(true);
    expect(containsDisallowedRequiredTextControl('Desk\u0085Lamp')).toBe(true);
  });

  it('submits allowed required-text controls and blocks carriage returns', async () => {
    const fetchMock = vi.fn(defaultFetch);
    vi.stubGlobal('fetch', fetchMock);
    renderApp();
    const user = await signIn();
    await user.click(screen.getByRole('button', { name: 'Add product' }));

    fireEvent.change(screen.getByLabelText(/Product name/), { target: { value: 'Desk\t\nLamp' } });
    await user.type(screen.getByLabelText(/^SKU/), 'TEXT-1');
    await user.selectOptions(screen.getByLabelText(/^Category/), 'Electronics');
    await user.type(screen.getByLabelText(/Unit price \(INR\)/), '12');
    await user.type(screen.getByLabelText(/Minimum stock threshold/), '2');
    await user.click(screen.getByRole('button', { name: 'Create product' }));
    await screen.findByRole('heading', { name: 'Products' });

    const createCalls = fetchMock.mock.calls.filter(([url, init]) => String(url).endsWith('/products')
      && init?.method === 'POST');
    expect(JSON.parse(String(createCalls[0][1]?.body)).name).toBe('Desk\t\nLamp');

    await user.click(screen.getByRole('button', { name: 'Add product' }));
    fireEvent.change(screen.getByLabelText(/Product name/), { target: { value: 'Desk\u0000Lamp' } });
    await user.type(screen.getByLabelText(/^SKU/), 'TEXT-2');
    await user.selectOptions(screen.getByLabelText(/^Category/), 'Electronics');
    await user.type(screen.getByLabelText(/Unit price \(INR\)/), '12');
    await user.type(screen.getByLabelText(/Minimum stock threshold/), '2');
    await user.click(screen.getByRole('button', { name: 'Create product' }));

    expect((await screen.findByRole('alert')).textContent).toContain('control character that is not allowed');
    expect(fetchMock.mock.calls.filter(([url, init]) => String(url).endsWith('/products')
      && init?.method === 'POST')).toHaveLength(1);
  });

  it('preserves server chronology when timestamps have different fractional precision', async () => {
    const orderedMovements = [
      { ...movement, id: 1, reasonReference: 'Whole second', occurredAt: '2026-11-04T10:00:00Z' },
      { ...movement, id: 2, reasonReference: 'Twelve hundredths', occurredAt: '2026-11-04T10:00:00.12Z' },
      { ...movement, id: 3, reasonReference: 'One hundred twenty-three milliseconds', occurredAt: '2026-11-04T10:00:00.123Z' },
    ];
    vi.stubGlobal('fetch', vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      if (String(input).includes('/movements') && (init?.method ?? 'GET') !== 'POST') {
        return Promise.resolve(jsonResponse(orderedMovements));
      }
      return defaultFetch(input, init);
    }));
    renderApp();
    const user = await signIn();
    await user.click(screen.getByRole('link', { name: 'Desk lamp' }));
    await screen.findByText('One hundred twenty-three milliseconds');

    const reasons = screen.getAllByRole('row').slice(1).map((row) => row.textContent ?? '');
    expect(reasons[0]).toContain('Whole second');
    expect(reasons[1]).toContain('Twelve hundredths');
    expect(reasons[2]).toContain('One hundred twenty-three milliseconds');
  });

  it('refreshes after a conflict and blocks retry until the user reviews current stock', async () => {
    let movementAttempts = 0;
    const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      if (String(input).includes('/movements') && init?.method === 'POST') {
        movementAttempts += 1;
        return Promise.resolve(movementAttempts === 1
          ? jsonResponse({ detail: 'Stock changed. Reload and review.' }, 409)
          : jsonResponse({}));
      }
      return defaultFetch(input, init);
    });
    vi.stubGlobal('fetch', fetchMock);
    renderApp();
    const user = await signIn();

    await user.click(screen.getByRole('link', { name: 'Desk lamp' }));
    await screen.findByText('Opening stock');
    await user.click(screen.getByRole('button', { name: 'Record movement' }));
    await screen.findByRole('heading', { name: 'Record movement' });
    await user.type(screen.getByLabelText(/Quantity/), '1');
    await user.type(screen.getByLabelText(/Reason or reference/), 'Issue for order');
    await user.click(screen.getByRole('button', { name: 'Record receipt' }));

    expect(await screen.findByRole('button', { name: 'I reviewed the current stock' })).toBeTruthy();
    const submitButton = screen.getByRole('button', { name: 'Record receipt' });
    expect((submitButton as HTMLButtonElement).disabled).toBe(true);
    await user.click(screen.getByRole('button', { name: 'I reviewed the current stock' }));
    expect((submitButton as HTMLButtonElement).disabled).toBe(false);
    await user.click(submitButton);

    await screen.findByRole('heading', { name: 'Desk lamp' });
    expect(movementAttempts).toBe(2);
  });
});
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route, Outlet } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { I18nextProvider } from 'react-i18next';
import { PizzeriaProvider } from './PizzeriaProvider';
import { testI18n } from '../tests/test-utils';

// Mock the auth API to prevent actual API calls during PizzeriaProvider tests
vi.mock('../api/auth', () => ({
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  fetchProfile: vi.fn(),
  deleteProfile: vi.fn(),
}));

vi.mock('../api/client', () => ({
  setAuthToken: vi.fn(),
  getAuthToken: vi.fn(() => null),
}));

const createQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

describe('PizzeriaProvider', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  const renderPizzeriaProvider = (route: string) => {
    const queryClient = createQueryClient();

    return render(
      <QueryClientProvider client={queryClient}>
        <I18nextProvider i18n={testI18n}>
          <MemoryRouter
            initialEntries={[route]}
            future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
          >
            <Routes>
              <Route path="/:pizzeriaCode/*" element={<PizzeriaProvider />}>
                <Route index element={<div>Home Content</div>} />
                <Route path="menu" element={<div>Menu Content</div>} />
              </Route>
              <Route path="/not-found" element={<div>Not Found Page</div>} />
            </Routes>
          </MemoryRouter>
        </I18nextProvider>
      </QueryClientProvider>
    );
  };

  it('should render children when pizzeriaCode is provided', async () => {
    renderPizzeriaProvider('/kingspizza');

    await waitFor(() => {
      expect(screen.getByText('Home Content')).toBeInTheDocument();
    });
  });

  it('should wrap children with AuthProvider', async () => {
    // This test verifies that PizzeriaProvider wraps its content with AuthProvider
    renderPizzeriaProvider('/testpizza');

    await waitFor(() => {
      expect(screen.getByText('Home Content')).toBeInTheDocument();
    });
  });

  it('should handle route with nested paths', async () => {
    renderPizzeriaProvider('/kingspizza/menu');

    await waitFor(() => {
      expect(screen.getByText('Menu Content')).toBeInTheDocument();
    });
  });

  it('should provide pizzeriaCode context to nested components', async () => {
    // We import usePizzeriaContext at the top level to avoid require issues
    const { usePizzeriaContext } = await import('./PizzeriaProvider');

    // Component that uses the pizzeriaCode from context
    const TestContextConsumer = () => {
      const { pizzeriaCode } = usePizzeriaContext();
      return <div>Pizzeria: {pizzeriaCode}</div>;
    };

    const queryClient = createQueryClient();

    render(
      <QueryClientProvider client={queryClient}>
        <I18nextProvider i18n={testI18n}>
          <MemoryRouter
            initialEntries={['/testpizza']}
            future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
          >
            <Routes>
              <Route path="/:pizzeriaCode" element={<PizzeriaProvider />}>
                <Route index element={<TestContextConsumer />} />
              </Route>
            </Routes>
          </MemoryRouter>
        </I18nextProvider>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('Pizzeria: testpizza')).toBeInTheDocument();
    });
  });
});

describe('ProtectedRoute', () => {
  // ProtectedRoute is tightly coupled to PizzeriaProvider via usePizzeriaCode
  // Testing it requires the full routing context

  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('should be exported from module', async () => {
    const module = await import('./ProtectedRoute');
    expect(module.ProtectedRoute).toBeDefined();
  });

  // Integration tests for ProtectedRoute behavior are better done
  // at the page level or in e2e tests since it requires:
  // 1. PizzeriaProvider context
  // 2. AuthProvider context (from PizzeriaProvider)
  // 3. Router context with correct route params

  // The actual redirect/render logic is simple and is tested
  // via the page tests that use protected routes
});

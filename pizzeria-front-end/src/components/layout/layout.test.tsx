import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { I18nextProvider } from 'react-i18next';
import { Container } from './Container';
import { Layout } from './Layout';
import { Header } from './Header';
import { PizzeriaProvider, PizzeriaContext } from '../../routes/PizzeriaProvider';
import { AuthContext } from '../../features/auth/AuthProvider';
import { CartProvider } from '../../features/cart/CartProvider';
import {
  testI18n,
  createMockAuthContext,
  createAuthenticatedMockAuthContext,
} from '../../tests/test-utils';
import { ConsoleCaptureContext } from '../../hooks/useConsoleCapture';
import type { ConsoleCaptureContextValue } from '../../hooks/useConsoleCapture';

const mockConsoleCaptureContext: ConsoleCaptureContextValue = {
  levelSettings: { error: true, warn: true, info: false, debug: false },
  setLevelEnabled: () => {},
  openPanel: () => {},
  hasLogs: false,
  errorCount: 0,
  warnCount: 0,
};

// Mock APIs
vi.mock('../../api/auth', () => ({
  fetchProfile: vi.fn(),
  logout: vi.fn(),
}));

vi.mock('../../api/client', () => ({
  setAuthToken: vi.fn(),
  getAuthToken: vi.fn(() => null),
}));

const createQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

describe('Container', () => {
  it('should render children', () => {
    render(<Container>Content</Container>);
    expect(screen.getByText('Content')).toBeInTheDocument();
  });

  it('should apply max-w-7xl class', () => {
    const { container } = render(<Container>Content</Container>);
    expect(container.firstChild).toHaveClass('max-w-7xl');
  });

  it('should apply custom className', () => {
    const { container } = render(<Container className="custom">Content</Container>);
    expect(container.firstChild).toHaveClass('custom');
  });

  it('should have responsive padding', () => {
    const { container } = render(<Container>Content</Container>);
    expect(container.firstChild).toHaveClass('px-4', 'sm:px-6', 'lg:px-8');
  });
});

describe('Layout', () => {
  const renderLayout = () => {
    const queryClient = createQueryClient();

    return render(
      <QueryClientProvider client={queryClient}>
        <I18nextProvider i18n={testI18n}>
          <ConsoleCaptureContext.Provider value={mockConsoleCaptureContext}>
            <MemoryRouter
              initialEntries={['/testpizza']}
              future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
            >
              <Routes>
                <Route path="/:pizzeriaCode/*" element={<PizzeriaProvider />}>
                  <Route path="*" element={<Layout />} />
                </Route>
              </Routes>
            </MemoryRouter>
          </ConsoleCaptureContext.Provider>
        </I18nextProvider>
      </QueryClientProvider>
    );
  };

  it('should render header with navigation', async () => {
    renderLayout();
    expect(screen.getByRole('navigation')).toBeInTheDocument();
  });

  it('should render footer with copyright', () => {
    renderLayout();

    const currentYear = new Date().getFullYear();
    expect(screen.getByText(new RegExp(`${currentYear}`))).toBeInTheDocument();
    // Footer now shows translated "footer.rights" key
    expect(screen.getByText(/footer.rights/)).toBeInTheDocument();
  });

  it('should have min-h-screen class', () => {
    const { container } = renderLayout();
    expect(container.querySelector('.min-h-screen')).toBeInTheDocument();
  });

  it('should show navigation links', () => {
    renderLayout();

    // Note: Home is the logo link, Menu and Pizzas are nav links
    expect(screen.getByRole('link', { name: /menu/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /pizzas/i })).toBeInTheDocument();
  });

  it('should show sign in when not authenticated', () => {
    renderLayout();
    expect(screen.getByRole('link', { name: /sign in/i })).toBeInTheDocument();
  });

  it('should have language selector button', () => {
    renderLayout();
    // Language button shows globe emoji and "Language" text
    const langButton = screen.getByRole('button', { name: /🌐.*language/i });
    expect(langButton).toBeInTheDocument();
  });
});

describe('Header Admin Dropdown', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockPizzeriaContext = {
    pizzeriaCode: 'testpizza',
    pizzeriaName: 'Test Pizzeria',
    isOpenNow: true,
    phoneNumbers: [],
    isLoading: false,
    openingHours: null,
    timezone: 'Europe/Stockholm',
    address: null,
  };

  const renderHeaderWithAuth = (
    isAdmin: boolean,
    pizzeriaCode: string = 'testpizza'
  ) => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    const authContext = isAdmin
      ? createAuthenticatedMockAuthContext({
          profile: {
            id: 'user-1',
            name: 'Admin User',
            email: 'admin@example.com',
            emailVerified: true,
            preferredDiet: 'NONE',
            preferredIngredientIds: [],
            pizzeriaAdmin: pizzeriaCode,
            profilePhotoBase64: null,
            createdAt: '2024-01-01T00:00:00Z',
            updatedAt: '2024-01-15T00:00:00Z',
          },
        })
      : createMockAuthContext();

    return render(
      <QueryClientProvider client={queryClient}>
        <I18nextProvider i18n={testI18n}>
          <ConsoleCaptureContext.Provider value={mockConsoleCaptureContext}>
            <MemoryRouter
              initialEntries={[`/${pizzeriaCode}`]}
              future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
            >
              <PizzeriaContext.Provider value={{ ...mockPizzeriaContext, pizzeriaCode }}>
                <AuthContext.Provider value={authContext}>
                  <CartProvider>
                    <Header />
                  </CartProvider>
                </AuthContext.Provider>
              </PizzeriaContext.Provider>
            </MemoryRouter>
          </ConsoleCaptureContext.Provider>
        </I18nextProvider>
      </QueryClientProvider>
    );
  };

  it('should not show admin dropdown for non-admin users', () => {
    renderHeaderWithAuth(false);

    expect(screen.queryByRole('button', { name: /admin/i })).not.toBeInTheDocument();
  });

  it('should show admin dropdown for admin users', () => {
    renderHeaderWithAuth(true);

    expect(screen.getByRole('button', { name: /admin/i })).toBeInTheDocument();
  });

  it('should show Price Management and Customer Feedback options when dropdown is open', async () => {
    const user = userEvent.setup();
    renderHeaderWithAuth(true);

    const adminButton = screen.getByRole('button', { name: /admin/i });
    await user.click(adminButton);

    expect(screen.getByRole('link', { name: /price management/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /customer feedback/i })).toBeInTheDocument();
  });

  it('should have correct links in admin dropdown', async () => {
    const user = userEvent.setup();
    renderHeaderWithAuth(true, 'testpizza');

    const adminButton = screen.getByRole('button', { name: /admin/i });
    await user.click(adminButton);

    const pricesLink = screen.getByRole('link', { name: /price management/i });
    const feedbackLink = screen.getByRole('link', { name: /customer feedback/i });

    expect(pricesLink).toHaveAttribute('href', '/testpizza/admin/prices');
    expect(feedbackLink).toHaveAttribute('href', '/testpizza/admin/feedback');
  });

  it('should close dropdown when clicking outside', async () => {
    const user = userEvent.setup();
    renderHeaderWithAuth(true);

    const adminButton = screen.getByRole('button', { name: /admin/i });
    await user.click(adminButton);

    // Verify dropdown is open
    expect(screen.getByRole('link', { name: /price management/i })).toBeInTheDocument();

    // Click outside the dropdown (on the header or body)
    await user.click(document.body);

    await waitFor(() => {
      expect(screen.queryByRole('link', { name: /price management/i })).not.toBeInTheDocument();
    });
  });
});

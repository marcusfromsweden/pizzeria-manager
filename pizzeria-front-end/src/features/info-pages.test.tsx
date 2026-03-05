import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { I18nextProvider } from 'react-i18next';
import { HomePage } from './home/HomePage';
import { NotFoundPage } from './error/NotFoundPage';
import { AuthContext } from './auth/AuthProvider';
import { PizzeriaProvider } from '../routes/PizzeriaProvider';
import { testI18n } from '../tests/test-utils';
import type { UserProfileResponse } from '../types/api';

// Mock APIs
vi.mock('../api/auth', () => ({
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  fetchProfile: vi.fn(),
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

const mockProfile: UserProfileResponse = {
  id: 'user-1',
  name: 'Test User',
  email: 'test@example.com',
  emailVerified: true,
  preferredDiet: 'NONE',
  preferredIngredientIds: [],
  pizzeriaAdmin: null,
  profilePhotoBase64: null,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-15T00:00:00Z',
};

interface MockAuthContext {
  isAuthenticated: boolean;
  isLoading: boolean;
  profile: UserProfileResponse | null;
  login: ReturnType<typeof vi.fn>;
  logout: ReturnType<typeof vi.fn>;
  register: ReturnType<typeof vi.fn>;
  deleteAccount: ReturnType<typeof vi.fn>;
  refreshProfile: ReturnType<typeof vi.fn>;
}

const createMockAuthContext = (
  overrides: Partial<MockAuthContext> = {}
): MockAuthContext => ({
  isAuthenticated: false,
  isLoading: false,
  profile: null,
  login: vi.fn(),
  logout: vi.fn(),
  register: vi.fn(),
  deleteAccount: vi.fn(),
  refreshProfile: vi.fn(),
  ...overrides,
});

describe('HomePage', () => {
  const renderHomePage = (mockAuth: MockAuthContext = createMockAuthContext()) => {
    const queryClient = createQueryClient();

    return render(
      <QueryClientProvider client={queryClient}>
        <I18nextProvider i18n={testI18n}>
          <MemoryRouter
            initialEntries={['/testpizza']}
            future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
          >
            <Routes>
              <Route path="/:pizzeriaCode/*" element={<PizzeriaProvider />}>
                <Route
                  index
                  element={
                    <AuthContext.Provider value={mockAuth}>
                      <HomePage />
                    </AuthContext.Provider>
                  }
                />
              </Route>
            </Routes>
          </MemoryRouter>
        </I18nextProvider>
      </QueryClientProvider>
    );
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render home page with pizzeria name', () => {
    renderHomePage();

    expect(screen.getByRole('heading', { name: /testpizza/i })).toBeInTheDocument();
    expect(screen.getByText('🍕')).toBeInTheDocument();
  });

  it('should show menu and pizzas links', () => {
    renderHomePage();

    // Multiple menu links exist (hero button and feature card)
    const menuLinks = screen.getAllByRole('link', { name: /menu/i });
    expect(menuLinks.length).toBeGreaterThan(0);
    expect(menuLinks[0]).toHaveAttribute('href', '/testpizza/menu');

    expect(screen.getByRole('link', { name: /pizzas/i })).toHaveAttribute(
      'href',
      '/testpizza/pizzas'
    );
  });

  it('should show feature cards', () => {
    renderHomePage();

    // Check for feature card headings (uses translation keys in test)
    expect(screen.getByText(/dietary\.title/i)).toBeInTheDocument();
    expect(screen.getByText(/scores\.title/i)).toBeInTheDocument();
  });

  it('should show sign in CTA when not authenticated', () => {
    renderHomePage();

    // CTA section exists (uses translation keys in test)
    expect(screen.getByText(/cta\.title/i)).toBeInTheDocument();
    // Multiple sign in links exist (feature cards and CTA section)
    const signInLinks = screen.getAllByRole('link', { name: /sign/i });
    expect(signInLinks.length).toBeGreaterThan(0);
  });

  it('should show sign in links in feature cards when not authenticated', () => {
    renderHomePage();

    // Multiple "Sign In →" buttons in feature cards
    const signInButtons = screen.getAllByRole('link', { name: /sign in/i });
    expect(signInButtons.length).toBeGreaterThan(0);
  });

  it('should not show CTA section when authenticated', () => {
    const mockAuth = createMockAuthContext({
      isAuthenticated: true,
      profile: mockProfile,
    });

    renderHomePage(mockAuth);

    expect(screen.queryByText(/cta\.title/i)).not.toBeInTheDocument();
  });

  it('should show preferences link when authenticated', () => {
    const mockAuth = createMockAuthContext({
      isAuthenticated: true,
      profile: mockProfile,
    });

    renderHomePage(mockAuth);

    // Check that preferences link exists
    const preferencesLink = screen.getByRole('link', { name: /dietary/i });
    expect(preferencesLink).toHaveAttribute('href', '/testpizza/preferences');
  });

  it('should show rate now link when authenticated', () => {
    const mockAuth = createMockAuthContext({
      isAuthenticated: true,
      profile: mockProfile,
    });

    renderHomePage(mockAuth);

    // Check that scores link exists
    const scoresLink = screen.getByRole('link', { name: /scores/i });
    expect(scoresLink).toHaveAttribute('href', '/testpizza/scores');
  });
});

describe('NotFoundPage', () => {
  const renderNotFoundPage = (path = '/testpizza/some-invalid-page') => {
    const queryClient = createQueryClient();
    const mockAuth = createMockAuthContext();

    return render(
      <QueryClientProvider client={queryClient}>
        <I18nextProvider i18n={testI18n}>
          <MemoryRouter
            initialEntries={[path]}
            future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
          >
            <AuthContext.Provider value={mockAuth}>
              <NotFoundPage />
            </AuthContext.Provider>
          </MemoryRouter>
        </I18nextProvider>
      </QueryClientProvider>
    );
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render 404 page', () => {
    renderNotFoundPage();

    expect(screen.getByRole('heading', { name: '404' })).toBeInTheDocument();
    expect(screen.getByText(/not found/i)).toBeInTheDocument();
  });

  it('should show pizza emoji', () => {
    renderNotFoundPage();

    expect(screen.getByText('🍕')).toBeInTheDocument();
  });

  it('should show home link with pizzeria code from path', () => {
    renderNotFoundPage('/testpizza/invalid');

    expect(screen.getByRole('link', { name: /home/i })).toHaveAttribute(
      'href',
      '/testpizza'
    );
  });

  it('should show menu link', () => {
    renderNotFoundPage('/testpizza/invalid');

    expect(screen.getByRole('link', { name: /menu/i })).toHaveAttribute(
      'href',
      '/testpizza/menu'
    );
  });

  it('should default to kingspizza when no pizzeria code in path', () => {
    renderNotFoundPage('/');

    expect(screen.getByRole('link', { name: /home/i })).toHaveAttribute(
      'href',
      '/kingspizza'
    );
  });

  it('should show helpful message', () => {
    renderNotFoundPage();

    // Uses translation key in test environment
    expect(screen.getByText(/notFound/i)).toBeInTheDocument();
  });
});

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { I18nextProvider } from 'react-i18next';
import { PizzaListPage } from './pizzas/PizzaListPage';
import { PizzaDetailPage } from './pizzas/PizzaDetailPage';
import { MenuPage } from './menu/MenuPage';
import { AuthContext, type AuthContextValue } from './auth/AuthProvider';
import { PizzeriaProvider } from '../routes/PizzeriaProvider';
import { testI18n } from '../tests/test-utils';
import * as pizzasApi from '../api/pizzas';
import * as menuApi from '../api/menu';
import * as authApi from '../api/auth';
import * as clientModule from '../api/client';
import type {
  PizzaSummaryResponse,
  PizzaDetailResponse,
  MenuResponse,
  PizzaSuitabilityResponse,
} from '../types/api';

// Mock APIs
vi.mock('../api/pizzas', () => ({
  fetchPizzas: vi.fn(),
  fetchPizza: vi.fn(),
  checkSuitability: vi.fn(),
}));

vi.mock('../api/menu', () => ({
  fetchMenu: vi.fn(),
}));

vi.mock('../api/auth', () => ({
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  fetchProfile: vi.fn(),
}));

vi.mock('../api/client', () => ({
  setAuthToken: vi.fn(),
  getAuthToken: vi.fn(() => null),
  getApiErrorMessage: vi.fn((err: unknown) =>
    err instanceof Error ? err.message : 'An error occurred'
  ),
}));

const mockPizzasApi = vi.mocked(pizzasApi);
const mockMenuApi = vi.mocked(menuApi);

const createQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

// Mock data
const mockPizzas: PizzaSummaryResponse[] = [
  {
    id: 'pizza-1',
    dishNumber: 1,
    nameKey: 'translation.key.disc.pizza.margarita',
    priceInSek: '95',
    familySizePriceInSek: '195',
    overallDietaryType: 'VEGETARIAN',
    sortOrder: 1,
  },
  {
    id: 'pizza-2',
    dishNumber: 2,
    nameKey: 'translation.key.disc.pizza.hawaii',
    priceInSek: '105',
    familySizePriceInSek: '205',
    overallDietaryType: 'CARNIVORE',
    sortOrder: 2,
  },
];

const mockPizzaDetail: PizzaDetailResponse = {
  id: 'pizza-1',
  dishNumber: 1,
  nameKey: 'translation.key.disc.pizza.margarita',
  descriptionKey: 'translation.key.description.margarita',
  priceInSek: '95',
  familySizePriceInSek: '195',
  ingredients: [
    { id: 'ing-1', ingredientKey: 'translation.key.ingredient.tomato_sauce', dietaryType: 'VEGAN', allergenTags: [], spiceLevel: 0 },
    { id: 'ing-2', ingredientKey: 'translation.key.ingredient.mozzarella', dietaryType: 'VEGETARIAN', allergenTags: ['DAIRY'], spiceLevel: 0 },
    { id: 'ing-3', ingredientKey: 'translation.key.ingredient.basil', dietaryType: 'VEGAN', allergenTags: [], spiceLevel: 0 },
  ],
  overallDietaryType: 'VEGETARIAN',
  sortOrder: 1,
};

const mockMenu: MenuResponse = {
  sections: [
    {
      id: 'section-1',
      code: 'pizza',
      translationKey: 'translation.key.section.pizza',
      sortOrder: 1,
      items: [
        {
          id: 'item-1',
          sectionId: 'section-1',
          dishNumber: 1,
          nameKey: 'translation.key.disc.pizza.margarita',
          descriptionKey: 'translation.key.description.margarita',
          priceInSek: '95',
          familySizePriceInSek: '195',
          sortOrder: 1,
          ingredients: [
            {
              id: 'ing-1',
              ingredientKey: 'translation.key.ingredient.mozzarella',
              dietaryType: 'VEGETARIAN',
              allergenTags: ['dairy'],
              spiceLevel: 0,
            },
          ],
          overallDietaryType: 'VEGETARIAN',
        },
      ],
    },
  ],
  pizzaCustomisations: [
    {
      id: 'custom-1',
      nameKey: 'translation.key.pizza.customisation.extra_cheese',
      priceInSek: '15',
      familySizePriceInSek: '25',
      sortOrder: 1,
    },
  ],
};

const mockSuitabilityResult: PizzaSuitabilityResponse = {
  suitable: true,
  violations: [],
  suggestions: [],
};

const createMockAuthContext = (
  overrides: Partial<AuthContextValue> = {}
): AuthContextValue => ({
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

describe('PizzaListPage', () => {
  const renderPizzaListPage = () => {
    const queryClient = createQueryClient();
    const mockAuth = createMockAuthContext();

    return render(
      <QueryClientProvider client={queryClient}>
        <I18nextProvider i18n={testI18n}>
          <MemoryRouter
            initialEntries={['/testpizza/pizzas']}
            future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
          >
            <Routes>
              <Route path="/:pizzeriaCode/*" element={<PizzeriaProvider />}>
                <Route
                  path="pizzas"
                  element={
                    <AuthContext.Provider value={mockAuth}>
                      <PizzaListPage />
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

  it('should show loading spinner initially', () => {
    mockPizzasApi.fetchPizzas.mockReturnValue(new Promise(() => {}) as never);

    renderPizzaListPage();

    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('should display pizzas when loaded', async () => {
    mockPizzasApi.fetchPizzas.mockResolvedValue({ data: mockPizzas } as never);

    renderPizzaListPage();

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /pizzas/i })).toBeInTheDocument();
    });

    // Check for pizza numbers
    expect(screen.getByText('#1')).toBeInTheDocument();
    expect(screen.getByText('#2')).toBeInTheDocument();

    // Check for prices (multiple elements with same price due to family size)
    expect(screen.getAllByText(/95/).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/105/).length).toBeGreaterThan(0);
  });

  it('should link pizzas to detail pages', async () => {
    mockPizzasApi.fetchPizzas.mockResolvedValue({ data: mockPizzas } as never);

    renderPizzaListPage();

    await waitFor(() => {
      expect(screen.getByText('#1')).toBeInTheDocument();
    });

    const links = screen.getAllByRole('link');
    expect(links[0]).toHaveAttribute('href', '/testpizza/pizzas/pizza-1');
    expect(links[1]).toHaveAttribute('href', '/testpizza/pizzas/pizza-2');
  });

  it('should show error message on fetch failure', async () => {
    mockPizzasApi.fetchPizzas.mockRejectedValue(new Error('Network error'));

    renderPizzaListPage();

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/error/i);
    });
  });

  it('should show empty message when no pizzas', async () => {
    mockPizzasApi.fetchPizzas.mockResolvedValue({ data: [] } as never);

    renderPizzaListPage();

    await waitFor(() => {
      // Uses translation key in test environment
      expect(screen.getByText(/pizzas\.empty/i)).toBeInTheDocument();
    });
  });

  it('should not show family price when familySizePriceInSek is null', async () => {
    const pizzasWithNullFamily: PizzaSummaryResponse[] = [
      {
        id: 'pizza-1',
        dishNumber: 1,
        nameKey: 'translation.key.disc.pizza.margarita',
        priceInSek: '95',
        familySizePriceInSek: null,
        overallDietaryType: 'VEGETARIAN',
        sortOrder: 1,
      },
    ];
    mockPizzasApi.fetchPizzas.mockResolvedValue({ data: pizzasWithNullFamily } as never);

    renderPizzaListPage();

    await waitFor(() => {
      expect(screen.getByText('#1')).toBeInTheDocument();
    });

    expect(screen.getAllByText(/95/).length).toBeGreaterThan(0);
    expect(screen.queryByText(/item\.familySize/)).not.toBeInTheDocument();
  });
});

describe('PizzaDetailPage', () => {
  const renderPizzaDetailPage = (
    mockAuth: AuthContextValue = createMockAuthContext()
  ) => {
    const queryClient = createQueryClient();

    return render(
      <QueryClientProvider client={queryClient}>
        <I18nextProvider i18n={testI18n}>
          <MemoryRouter
            initialEntries={['/testpizza/pizzas/pizza-1']}
            future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
          >
            <Routes>
              <Route path="/:pizzeriaCode/*" element={<PizzeriaProvider />}>
                <Route
                  path="pizzas/:pizzaId"
                  element={
                    <AuthContext.Provider value={mockAuth}>
                      <PizzaDetailPage />
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

  it('should show loading spinner initially', () => {
    mockPizzasApi.fetchPizza.mockReturnValue(new Promise(() => {}) as never);

    renderPizzaDetailPage();

    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('should display pizza details when loaded', async () => {
    mockPizzasApi.fetchPizza.mockResolvedValue({ data: mockPizzaDetail } as never);

    renderPizzaDetailPage();

    await waitFor(() => {
      expect(screen.getByText('#1')).toBeInTheDocument();
    });

    // Check for ingredients
    expect(screen.getByText(/tomato sauce/i)).toBeInTheDocument();
    expect(screen.getByText(/mozzarella/i)).toBeInTheDocument();
  });

  it('should show back link to pizza list', async () => {
    mockPizzasApi.fetchPizza.mockResolvedValue({ data: mockPizzaDetail } as never);

    renderPizzaDetailPage();

    await waitFor(() => {
      expect(screen.getByText('#1')).toBeInTheDocument();
    });

    expect(screen.getByRole('link', { name: /back/i })).toHaveAttribute(
      'href',
      '/testpizza/pizzas'
    );
  });

  it('should show login prompt for suitability check when not authenticated', async () => {
    mockPizzasApi.fetchPizza.mockResolvedValue({ data: mockPizzaDetail } as never);

    renderPizzaDetailPage();

    await waitFor(() => {
      expect(screen.getByText(/sign in to check dietary suitability/i)).toBeInTheDocument();
    });

    expect(screen.getByRole('link', { name: /sign in/i })).toHaveAttribute(
      'href',
      '/testpizza/login'
    );
  });

  it('should show suitability check button when authenticated', async () => {
    mockPizzasApi.fetchPizza.mockResolvedValue({ data: mockPizzaDetail } as never);

    const mockAuth = createMockAuthContext({
      isAuthenticated: true,
      profile: {
        id: 'user-1',
        name: 'Test User',
        email: 'test@example.com',
        emailVerified: true,
        preferredDiet: 'NONE',
        preferredIngredientIds: [],
        pizzeriaAdmin: null,
        profilePhotoBase64: null,
        createdAt: '2024-01-01',
        updatedAt: '2024-01-01',
      },
    });

    renderPizzaDetailPage(mockAuth);

    await waitFor(() => {
      expect(
        screen.getByRole('button', { name: /check suitability/i })
      ).toBeInTheDocument();
    });
  });

  it('should check suitability when button is clicked', async () => {
    const user = userEvent.setup();
    mockPizzasApi.fetchPizza.mockResolvedValue({ data: mockPizzaDetail } as never);
    mockPizzasApi.checkSuitability.mockResolvedValue({
      data: mockSuitabilityResult,
    } as never);

    const mockAuth = createMockAuthContext({
      isAuthenticated: true,
      profile: {
        id: 'user-1',
        name: 'Test User',
        email: 'test@example.com',
        emailVerified: true,
        preferredDiet: 'NONE',
        preferredIngredientIds: [],
        pizzeriaAdmin: null,
        profilePhotoBase64: null,
        createdAt: '2024-01-01',
        updatedAt: '2024-01-01',
      },
    });

    renderPizzaDetailPage(mockAuth);

    await waitFor(() => {
      expect(
        screen.getByRole('button', { name: /check suitability/i })
      ).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /check suitability/i }));

    await waitFor(() => {
      expect(screen.getByText(/suitable for your diet/i)).toBeInTheDocument();
    });
  });

  it('should show error message on fetch failure', async () => {
    mockPizzasApi.fetchPizza.mockRejectedValue(new Error('Not found'));

    renderPizzaDetailPage();

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/error/i);
    });
  });

  it('should not show family price row when familySizePriceInSek is null', async () => {
    const pizzaNoFamily: PizzaDetailResponse = {
      ...mockPizzaDetail,
      familySizePriceInSek: null,
    };
    mockPizzasApi.fetchPizza.mockResolvedValue({ data: pizzaNoFamily } as never);

    renderPizzaDetailPage();

    await waitFor(() => {
      expect(screen.getByText('#1')).toBeInTheDocument();
    });

    // Regular price should still show
    expect(screen.getAllByText(/95/).length).toBeGreaterThan(0);
    // Family size label should not appear
    expect(screen.queryByText(/item\.familySize/)).not.toBeInTheDocument();
  });
});

describe('MenuPage', () => {
  const renderMenuPage = () => {
    const queryClient = createQueryClient();
    const mockAuth = createMockAuthContext();

    return render(
      <QueryClientProvider client={queryClient}>
        <I18nextProvider i18n={testI18n}>
          <MemoryRouter
            initialEntries={['/testpizza/menu']}
            future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
          >
            <Routes>
              <Route path="/:pizzeriaCode/*" element={<PizzeriaProvider />}>
                <Route
                  path="menu"
                  element={
                    <AuthContext.Provider value={mockAuth}>
                      <MenuPage />
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

  it('should show loading spinner initially', () => {
    mockMenuApi.fetchMenu.mockReturnValue(new Promise(() => {}) as never);

    renderMenuPage();

    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('should display menu sections when loaded', async () => {
    mockMenuApi.fetchMenu.mockResolvedValue({ data: mockMenu } as never);

    renderMenuPage();

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /our menu/i })).toBeInTheDocument();
    });

    // Section headers are buttons (expandable) - "Pizza" (singular from translation key)
    expect(screen.getByRole('button', { name: /pizza/i })).toBeInTheDocument();
  });

  it('should expand section when clicked', async () => {
    const user = userEvent.setup();
    mockMenuApi.fetchMenu.mockResolvedValue({ data: mockMenu } as never);

    renderMenuPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /pizza/i })).toBeInTheDocument();
    });

    // Initially collapsed - no item links visible
    expect(screen.queryByRole('link')).not.toBeInTheDocument();

    // Click to expand
    await user.click(screen.getByRole('button', { name: /pizza/i }));

    // Now item link should be visible
    await waitFor(() => {
      expect(screen.getByRole('link')).toBeInTheDocument();
    });
  });

  it('should show customisations section', async () => {
    mockMenuApi.fetchMenu.mockResolvedValue({ data: mockMenu } as never);

    renderMenuPage();

    await waitFor(() => {
      expect(screen.getByText(/customisations/i)).toBeInTheDocument();
    });

    // Customisation price
    expect(screen.getByText(/\+15 SEK/)).toBeInTheDocument();
  });

  it('should show error message on fetch failure', async () => {
    mockMenuApi.fetchMenu.mockRejectedValue(new Error('Network error'));

    renderMenuPage();

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/error/i);
    });
  });

  it('should collapse expanded section when clicked again', async () => {
    const user = userEvent.setup();
    mockMenuApi.fetchMenu.mockResolvedValue({ data: mockMenu } as never);

    renderMenuPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /pizza/i })).toBeInTheDocument();
    });

    // Expand
    await user.click(screen.getByRole('button', { name: /pizza/i }));

    await waitFor(() => {
      expect(screen.getByRole('link')).toBeInTheDocument();
    });

    // Collapse
    await user.click(screen.getByRole('button', { name: /pizza/i }));

    await waitFor(() => {
      expect(screen.queryByRole('link')).not.toBeInTheDocument();
    });
  });
});

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { I18nextProvider } from 'react-i18next';
import { ProfilePage } from './profile/ProfilePage';
import { PreferencesPage } from './preferences/PreferencesPage';
import { ScoresPage } from './scores/ScoresPage';
import { FeedbackPage } from './feedback/FeedbackPage';
import { AuthContext, type AuthContextValue } from './auth/AuthProvider';
import { PizzeriaProvider } from '../routes/PizzeriaProvider';
import { testI18n } from '../tests/test-utils';
import * as authApi from '../api/auth';
import * as preferencesApi from '../api/preferences';
import * as scoresApi from '../api/scores';
import * as feedbackApi from '../api/feedback';
import * as pizzasApi from '../api/pizzas';
import * as menuApi from '../api/menu';
import * as clientModule from '../api/client';
import type { UserProfileResponse } from '../types/api';

// Mock APIs
vi.mock('../api/auth', () => ({
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  fetchProfile: vi.fn(),
  updateProfile: vi.fn(),
  deleteProfile: vi.fn(),
}));

vi.mock('../api/preferences', () => ({
  fetchDiet: vi.fn(),
  updateDiet: vi.fn(),
  fetchPreferredIngredients: vi.fn(),
  addPreferredIngredient: vi.fn(),
  removePreferredIngredient: vi.fn(),
}));

vi.mock('../api/scores', () => ({
  fetchMyScores: vi.fn(),
  createScore: vi.fn(),
}));

vi.mock('../api/feedback', () => ({
  submitServiceFeedback: vi.fn(),
}));

vi.mock('../api/pizzas', () => ({
  fetchPizzas: vi.fn(),
  fetchPizza: vi.fn(),
}));

vi.mock('../api/menu', () => ({
  fetchMenu: vi.fn(),
}));

vi.mock('../api/client', () => ({
  setAuthToken: vi.fn(),
  getAuthToken: vi.fn(() => null),
  getApiErrorMessage: vi.fn((err: unknown) =>
    err instanceof Error ? err.message : 'An error occurred'
  ),
}));

const mockAuthApi = vi.mocked(authApi);
const mockPreferencesApi = vi.mocked(preferencesApi);
const mockScoresApi = vi.mocked(scoresApi);
const mockFeedbackApi = vi.mocked(feedbackApi);
const mockPizzasApi = vi.mocked(pizzasApi);
const mockMenuApi = vi.mocked(menuApi);
const mockClientModule = vi.mocked(clientModule);

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

// Create a type that matches the actual AuthContext interface
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
  isAuthenticated: true,
  isLoading: false,
  profile: mockProfile,
  login: vi.fn(),
  logout: vi.fn(),
  register: vi.fn(),
  deleteAccount: vi.fn(),
  refreshProfile: vi.fn(),
  ...overrides,
});

describe('ProfilePage', () => {
  const renderProfilePage = (mockAuth: MockAuthContext = createMockAuthContext()) => {
    const queryClient = createQueryClient();

    return render(
      <QueryClientProvider client={queryClient}>
        <I18nextProvider i18n={testI18n}>
          <MemoryRouter
            initialEntries={['/testpizza/profile']}
            future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
          >
            <Routes>
              <Route path="/:pizzeriaCode/*" element={<PizzeriaProvider />}>
                <Route
                  path="profile"
                  element={
                    <AuthContext.Provider value={mockAuth}>
                      <ProfilePage />
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

  it('should render profile form with user data', () => {
    renderProfilePage();

    expect(screen.getByRole('heading', { name: /my profile/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/name/i)).toHaveValue('Test User');
    expect(screen.getByText('test@example.com')).toBeInTheDocument();
    expect(screen.getByText(/email verified/i)).toBeInTheDocument();
  });

  it('should show email not verified badge when email is not verified', () => {
    const mockAuth = createMockAuthContext({
      profile: { ...mockProfile, emailVerified: false },
    });

    renderProfilePage(mockAuth);

    expect(screen.getByText(/email not verified/i)).toBeInTheDocument();
  });

  it('should update profile on form submit', async () => {
    const user = userEvent.setup();
    const mockRefreshProfile = vi.fn();
    const mockAuth = createMockAuthContext({ refreshProfile: mockRefreshProfile });
    mockAuthApi.updateProfile.mockResolvedValue({} as never);

    renderProfilePage(mockAuth);

    const nameInput = screen.getByLabelText(/name/i);
    await user.clear(nameInput);
    await user.type(nameInput, 'New Name');

    await user.click(screen.getByRole('button', { name: /save/i }));

    await waitFor(() => {
      expect(mockAuthApi.updateProfile).toHaveBeenCalledWith({
        name: 'New Name',
        phone: undefined,
      });
    });
  });

  it('should show success message after profile update', async () => {
    const user = userEvent.setup();
    const mockRefreshProfile = vi.fn();
    const mockAuth = createMockAuthContext({ refreshProfile: mockRefreshProfile });
    mockAuthApi.updateProfile.mockResolvedValue({} as never);

    renderProfilePage(mockAuth);

    await user.click(screen.getByRole('button', { name: /save/i }));

    await waitFor(() => {
      expect(screen.getByText(/profile updated successfully/i)).toBeInTheDocument();
    });
  });

  it('should show delete account confirmation', async () => {
    const user = userEvent.setup();
    renderProfilePage();

    await user.click(screen.getByRole('button', { name: /delete my account/i }));

    expect(screen.getByText(/are you sure you want to delete/i)).toBeInTheDocument();
  });

  it('should call deleteAccount and navigate on confirm', async () => {
    const user = userEvent.setup();
    const mockDeleteAccount = vi.fn().mockResolvedValue(undefined);
    const mockAuth = createMockAuthContext({ deleteAccount: mockDeleteAccount });

    renderProfilePage(mockAuth);

    // First click to show confirmation
    await user.click(screen.getByRole('button', { name: /delete my account/i }));

    // Confirm deletion
    const deleteButtons = screen.getAllByRole('button', { name: /delete my account/i });
    await user.click(deleteButtons[0]); // Click the confirm button

    await waitFor(() => {
      expect(mockDeleteAccount).toHaveBeenCalled();
    });
  });

  it('should hide confirmation on cancel', async () => {
    const user = userEvent.setup();
    renderProfilePage();

    await user.click(screen.getByRole('button', { name: /delete my account/i }));
    expect(screen.getByText(/are you sure you want to delete/i)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /cancel/i }));

    expect(screen.queryByText(/are you sure you want to delete/i)).not.toBeInTheDocument();
  });
});

describe('PreferencesPage', () => {
  const renderPreferencesPage = () => {
    const queryClient = createQueryClient();
    const mockAuth = createMockAuthContext();

    return render(
      <QueryClientProvider client={queryClient}>
        <I18nextProvider i18n={testI18n}>
          <MemoryRouter
            initialEntries={['/testpizza/preferences']}
            future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
          >
            <Routes>
              <Route path="/:pizzeriaCode/*" element={<PizzeriaProvider />}>
                <Route
                  path="preferences"
                  element={
                    <AuthContext.Provider value={mockAuth}>
                      <PreferencesPage />
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
    // Setup default mocks
    mockPreferencesApi.fetchDiet.mockResolvedValue({ data: { diet: 'NONE' } } as never);
    mockPreferencesApi.fetchPreferredIngredients.mockResolvedValue({ data: [] } as never);
    mockMenuApi.fetchMenu.mockResolvedValue({
      data: {
        sections: [],
        pizzaCustomisations: [],
      },
    } as never);
  });

  it('should show loading state initially', () => {
    mockPreferencesApi.fetchDiet.mockReturnValue(new Promise(() => {}) as never);
    mockPreferencesApi.fetchPreferredIngredients.mockReturnValue(new Promise(() => {}) as never);

    renderPreferencesPage();

    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('should render preferences page with diet selector', async () => {
    renderPreferencesPage();

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /my preferences/i })).toBeInTheDocument();
    });

    expect(screen.getByRole('heading', { name: /dietary preference/i })).toBeInTheDocument();
    // Page has 2 comboboxes (diet and ingredients)
    expect(screen.getAllByRole('combobox').length).toBe(2);
  });

  it('should show empty state for preferred ingredients', async () => {
    renderPreferencesPage();

    await waitFor(() => {
      expect(screen.getByText(/no preferred ingredients selected/i)).toBeInTheDocument();
    });
  });

  it('should update diet when selection changes', async () => {
    const user = userEvent.setup();
    mockPreferencesApi.updateDiet.mockResolvedValue({} as never);

    renderPreferencesPage();

    await waitFor(() => {
      expect(screen.getAllByRole('combobox').length).toBe(2);
    });

    // Diet selector is the first combobox
    const selects = screen.getAllByRole('combobox');
    await user.selectOptions(selects[0], 'VEGAN');

    await waitFor(() => {
      expect(mockPreferencesApi.updateDiet).toHaveBeenCalledWith({ diet: 'VEGAN' });
    });
  });
});

describe('ScoresPage', () => {
  const renderScoresPage = () => {
    const queryClient = createQueryClient();
    const mockAuth = createMockAuthContext();

    return render(
      <QueryClientProvider client={queryClient}>
        <I18nextProvider i18n={testI18n}>
          <MemoryRouter
            initialEntries={['/testpizza/scores']}
            future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
          >
            <Routes>
              <Route path="/:pizzeriaCode/*" element={<PizzeriaProvider />}>
                <Route
                  path="scores"
                  element={
                    <AuthContext.Provider value={mockAuth}>
                      <ScoresPage />
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
    mockPizzasApi.fetchPizzas.mockResolvedValue({
      data: [
        {
          id: 'pizza-1',
          dishNumber: 1,
          nameKey: 'translation.key.disc.pizza.margarita',
          priceInSek: '95',
          familySizePriceInSek: '195',
          sortOrder: 1,
        },
      ],
    } as never);
    mockScoresApi.fetchMyScores.mockResolvedValue({ data: [] } as never);
  });

  it('should show loading state initially', () => {
    mockScoresApi.fetchMyScores.mockReturnValue(new Promise(() => {}) as never);

    renderScoresPage();

    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('should render scores page with form', async () => {
    renderScoresPage();

    await waitFor(() => {
      // "My Pizza Ratings" appears twice (h1 page title and h2 section header)
      expect(screen.getAllByRole('heading', { name: /my pizza ratings/i }).length).toBeGreaterThan(0);
    });

    expect(screen.getByText(/add rating/i)).toBeInTheDocument();
    expect(screen.getByText(/select a pizza/i)).toBeInTheDocument();
  });

  it('should show no scores message when empty', async () => {
    renderScoresPage();

    await waitFor(() => {
      expect(screen.getByText(/haven't rated any pizzas/i)).toBeInTheDocument();
    });
  });

  it('should display existing scores', async () => {
    mockScoresApi.fetchMyScores.mockResolvedValue({
      data: [
        {
          id: 'score-1',
          userId: 'user-1',
          pizzaId: 'pizza-1',
          pizzaType: 'TEMPLATE',
          score: 5,
          comment: 'Delicious!',
          createdAt: '2024-01-15T00:00:00Z',
        },
      ],
    } as never);

    renderScoresPage();

    await waitFor(() => {
      expect(screen.getByText('Delicious!')).toBeInTheDocument();
    });
  });

  it('should submit score', async () => {
    const user = userEvent.setup();
    mockScoresApi.createScore.mockResolvedValue({} as never);

    renderScoresPage();

    await waitFor(() => {
      expect(screen.getByText(/add rating/i)).toBeInTheDocument();
    });

    // Select pizza
    const pizzaSelect = screen.getAllByRole('combobox')[0];
    await user.selectOptions(pizzaSelect, 'pizza-1');

    // Submit
    await user.click(screen.getByRole('button', { name: /submit rating/i }));

    await waitFor(() => {
      expect(mockScoresApi.createScore).toHaveBeenCalled();
    });
  });
});

describe('FeedbackPage', () => {
  const renderFeedbackPage = () => {
    const queryClient = createQueryClient();
    const mockAuth = createMockAuthContext();

    return render(
      <QueryClientProvider client={queryClient}>
        <I18nextProvider i18n={testI18n}>
          <MemoryRouter
            initialEntries={['/testpizza/feedback']}
            future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
          >
            <Routes>
              <Route path="/:pizzeriaCode/*" element={<PizzeriaProvider />}>
                <Route
                  path="feedback"
                  element={
                    <AuthContext.Provider value={mockAuth}>
                      <FeedbackPage />
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

  it('should render feedback form', () => {
    renderFeedbackPage();

    expect(screen.getByRole('heading', { name: /service feedback/i })).toBeInTheDocument();
    expect(screen.getByText(/your feedback/i)).toBeInTheDocument();
    // Textarea has implicit role textbox when not associated with aria-describedby
    expect(screen.getByPlaceholderText(/tell us about your experience/i)).toBeInTheDocument();
  });

  it('should have disabled submit button when message is empty', () => {
    renderFeedbackPage();

    expect(screen.getByRole('button', { name: /send feedback/i })).toBeDisabled();
  });

  it('should enable submit button when message is entered', async () => {
    const user = userEvent.setup();
    renderFeedbackPage();

    await user.type(screen.getByPlaceholderText(/tell us about your experience/i), 'Great service!');

    expect(screen.getByRole('button', { name: /send feedback/i })).not.toBeDisabled();
  });

  it('should submit feedback', async () => {
    const user = userEvent.setup();
    mockFeedbackApi.submitServiceFeedback.mockResolvedValue({} as never);

    renderFeedbackPage();

    await user.type(screen.getByPlaceholderText(/tell us about your experience/i), 'Great service!');
    await user.click(screen.getByRole('button', { name: /send feedback/i }));

    await waitFor(() => {
      expect(mockFeedbackApi.submitServiceFeedback).toHaveBeenCalledWith({
        message: 'Great service!',
        rating: null,
        category: null,
      });
    });
  });

  it('should show success message after submission', async () => {
    const user = userEvent.setup();
    mockFeedbackApi.submitServiceFeedback.mockResolvedValue({} as never);

    renderFeedbackPage();

    await user.type(screen.getByPlaceholderText(/tell us about your experience/i), 'Great service!');
    await user.click(screen.getByRole('button', { name: /send feedback/i }));

    await waitFor(() => {
      expect(screen.getByText(/thank you for your feedback/i)).toBeInTheDocument();
    });
  });

  it('should show error message on submission failure', async () => {
    const user = userEvent.setup();
    mockFeedbackApi.submitServiceFeedback.mockRejectedValue(new Error('Network error'));
    mockClientModule.getApiErrorMessage.mockReturnValue('Network error');

    renderFeedbackPage();

    await user.type(screen.getByPlaceholderText(/tell us about your experience/i), 'Great service!');
    await user.click(screen.getByRole('button', { name: /send feedback/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Network error');
    });
  });
});

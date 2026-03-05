import { vi } from 'vitest';
import type { AxiosResponse } from 'axios';
import type {
  UserProfileResponse,
  UserLoginResponse,
  UserRegisterResponse,
  MenuResponse,
  MenuSectionResponse,
  PizzaSummaryResponse,
  PizzaDetailResponse,
  DietPreferenceResponse,
  MenuIngredientResponse,
  PizzaScoreResponse,
  FeedbackResponse,
  PizzaSuitabilityResponse,
} from '../../types/api';

// Helper to create mock axios response
export const createMockResponse = <T>(data: T, status = 200): AxiosResponse<T> => ({
  data,
  status,
  statusText: 'OK',
  headers: {},
  config: {
    headers: {},
  } as AxiosResponse['config'],
});

// Mock user profile
export const mockUserProfile: UserProfileResponse = {
  id: 'user-123',
  name: 'Test User',
  email: 'test@example.com',
  emailVerified: true,
  preferredDiet: 'NONE',
  preferredIngredientIds: [],
  pizzeriaAdmin: null,
  profilePhotoBase64: null,
  createdAt: '2024-01-15T12:00:00Z',
  updatedAt: '2024-01-15T12:00:00Z',
};

// Mock login response
export const mockLoginResponse: UserLoginResponse = {
  accessToken: 'mock-access-token-123',
};

// Mock register response
export const mockRegisterResponse: UserRegisterResponse = {
  userId: 'user-456',
  emailVerified: false,
  verificationToken: 'mock-verification-token-456',
};

// Mock ingredient
export const mockIngredient: MenuIngredientResponse = {
  id: 'ing-1',
  ingredientKey: 'translation.key.ingredient.tomato',
  dietaryType: 'VEGAN',
  allergenTags: [],
  spiceLevel: 0,
};

// Mock pizza summary
export const mockPizzaSummary: PizzaSummaryResponse = {
  id: 'pizza-1',
  dishNumber: 1,
  nameKey: 'translation.key.pizza.margherita',
  priceInSek: '95.00',
  familySizePriceInSek: '150.00',
  overallDietaryType: 'VEGETARIAN',
  sortOrder: 1,
};

// Mock pizza detail
export const mockPizzaDetail: PizzaDetailResponse = {
  id: 'pizza-1',
  dishNumber: 1,
  nameKey: 'translation.key.pizza.margherita',
  descriptionKey: 'translation.key.pizza.margherita.description',
  priceInSek: '95.00',
  familySizePriceInSek: '150.00',
  ingredients: [
    { id: 'ing-1', ingredientKey: 'translation.key.ingredient.tomato', dietaryType: 'VEGAN', allergenTags: [], spiceLevel: 0 },
    { id: 'ing-2', ingredientKey: 'translation.key.ingredient.mozzarella', dietaryType: 'VEGETARIAN', allergenTags: ['DAIRY'], spiceLevel: 0 },
  ],
  overallDietaryType: 'VEGETARIAN',
  sortOrder: 1,
};

// Mock menu section
export const mockMenuSection: MenuSectionResponse = {
  id: 'section-1',
  code: 'pizzas',
  translationKey: 'translation.key.section.pizzas',
  sortOrder: 1,
  items: [
    {
      id: 'item-1',
      sectionId: 'section-1',
      dishNumber: 1,
      nameKey: 'translation.key.pizza.margherita',
      descriptionKey: 'translation.key.pizza.margherita.description',
      priceInSek: '95.00',
      familySizePriceInSek: '150.00',
      ingredients: [mockIngredient],
      overallDietaryType: 'VEGAN',
      sortOrder: 1,
    },
  ],
};

// Mock menu response
export const mockMenuResponse: MenuResponse = {
  sections: [mockMenuSection],
  pizzaCustomisations: [
    {
      id: 'custom-1',
      nameKey: 'translation.key.customisation.extra_cheese',
      priceInSek: '15.00',
      familySizePriceInSek: '25.00',
      sortOrder: 1,
    },
  ],
};

// Mock diet response
export const mockDietResponse: DietPreferenceResponse = {
  diet: 'VEGETARIAN',
};

// Mock preferred ingredients (as array of ingredient responses)
export const mockPreferredIngredients: MenuIngredientResponse[] = [mockIngredient];

// Mock pizza score response
export const mockPizzaScoreResponse: PizzaScoreResponse = {
  id: 'score-1',
  userId: 'user-123',
  pizzaId: 'pizza-1',
  pizzaType: 'TEMPLATE',
  score: 5,
  comment: 'Great pizza!',
  createdAt: '2024-01-15T12:00:00Z',
};

// Mock feedback response
export const mockFeedbackResponse: FeedbackResponse = {
  id: 'feedback-1',
  userId: 'user-123',
  type: 'SERVICE',
  message: 'Great service!',
  rating: 5,
  category: 'Service',
  adminReply: null,
  adminRepliedAt: null,
  adminReplyReadAt: null,
  createdAt: '2024-01-15T12:00:00Z',
};

// Mock suitability response
export const mockSuitabilityResponse: PizzaSuitabilityResponse = {
  suitable: true,
  violations: [],
  suggestions: [],
};

// Create mock auth API module
export const createMockAuthApi = () => ({
  login: vi.fn().mockResolvedValue(createMockResponse(mockLoginResponse)),
  register: vi.fn().mockResolvedValue(createMockResponse(mockRegisterResponse)),
  verifyEmail: vi.fn().mockResolvedValue(createMockResponse({})),
  logout: vi.fn().mockResolvedValue(createMockResponse({})),
  fetchProfile: vi.fn().mockResolvedValue(createMockResponse(mockUserProfile)),
  updateProfile: vi.fn().mockResolvedValue(createMockResponse(mockUserProfile)),
  deleteProfile: vi.fn().mockResolvedValue(createMockResponse({})),
});

// Create mock menu API module
export const createMockMenuApi = () => ({
  fetchMenu: vi.fn().mockResolvedValue(createMockResponse(mockMenuResponse)),
});

// Create mock pizzas API module
export const createMockPizzasApi = () => ({
  fetchPizzas: vi.fn().mockResolvedValue(createMockResponse([mockPizzaSummary])),
  fetchPizza: vi.fn().mockResolvedValue(createMockResponse(mockPizzaDetail)),
  checkSuitability: vi.fn().mockResolvedValue(createMockResponse(mockSuitabilityResponse)),
});

// Create mock preferences API module
export const createMockPreferencesApi = () => ({
  fetchDiet: vi.fn().mockResolvedValue(createMockResponse(mockDietResponse)),
  updateDiet: vi.fn().mockResolvedValue(createMockResponse(mockDietResponse)),
  fetchPreferredIngredients: vi.fn().mockResolvedValue(createMockResponse({ ingredients: mockPreferredIngredients })),
  addPreferredIngredient: vi.fn().mockResolvedValue(createMockResponse({})),
  removePreferredIngredient: vi.fn().mockResolvedValue(createMockResponse({})),
});

// Create mock feedback API module
export const createMockFeedbackApi = () => ({
  submitServiceFeedback: vi.fn().mockResolvedValue(createMockResponse(mockFeedbackResponse)),
});

// Create mock scores API module
export const createMockScoresApi = () => ({
  createScore: vi.fn().mockResolvedValue(createMockResponse(mockPizzaScoreResponse)),
  fetchMyScores: vi.fn().mockResolvedValue(createMockResponse([mockPizzaScoreResponse])),
});

import { describe, it, expect, vi, beforeEach, type Mock } from 'vitest';
import api from './client';
import * as authApi from './auth';
import * as menuApi from './menu';
import * as pizzasApi from './pizzas';
import * as preferencesApi from './preferences';
import * as feedbackApi from './feedback';
import * as scoresApi from './scores';

// Mock the api client
vi.mock('./client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

// Type the mocked API methods
const mockApi = api as unknown as {
  get: Mock;
  post: Mock;
  put: Mock;
  patch: Mock;
  delete: Mock;
};

describe('API Modules', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('auth.ts', () => {
    describe('register', () => {
      it('should POST to correct endpoint with payload', async () => {
        const mockResponse = { data: { userId: '123', emailVerified: false, verificationToken: 'token' } };
        mockApi.post.mockResolvedValue(mockResponse);

        const payload = { name: 'John', email: 'john@example.com', password: 'password123' };
        const result = await authApi.register('kingspizza', payload);

        expect(mockApi.post).toHaveBeenCalledWith(
          '/pizzerias/kingspizza/users/register',
          payload
        );
        expect(result).toEqual(mockResponse);
      });
    });

    describe('verifyEmail', () => {
      it('should POST to correct endpoint with token', async () => {
        const mockResponse = { data: undefined };
        mockApi.post.mockResolvedValue(mockResponse);

        const payload = { token: 'verification-token' };
        const result = await authApi.verifyEmail('kingspizza', payload);

        expect(mockApi.post).toHaveBeenCalledWith(
          '/pizzerias/kingspizza/users/verify-email',
          payload
        );
        expect(result).toEqual(mockResponse);
      });
    });

    describe('login', () => {
      it('should POST to correct endpoint with credentials', async () => {
        const mockResponse = { data: { accessToken: 'jwt-token' } };
        mockApi.post.mockResolvedValue(mockResponse);

        const payload = { email: 'john@example.com', password: 'password123' };
        const result = await authApi.login('kingspizza', payload);

        expect(mockApi.post).toHaveBeenCalledWith(
          '/pizzerias/kingspizza/users/login',
          payload
        );
        expect(result).toEqual(mockResponse);
      });
    });

    describe('logout', () => {
      it('should POST to logout endpoint', async () => {
        const mockResponse = { data: undefined };
        mockApi.post.mockResolvedValue(mockResponse);

        const result = await authApi.logout();

        expect(mockApi.post).toHaveBeenCalledWith('/users/logout');
        expect(result).toEqual(mockResponse);
      });
    });

    describe('fetchProfile', () => {
      it('should GET user profile', async () => {
        const mockResponse = {
          data: {
            id: 'user-123',
            name: 'John',
            email: 'john@example.com',
            emailVerified: true,
            preferredDiet: 'NONE',
            preferredIngredientIds: [],
            createdAt: '2024-01-01',
            updatedAt: '2024-01-01',
          },
        };
        mockApi.get.mockResolvedValue(mockResponse);

        const result = await authApi.fetchProfile();

        expect(mockApi.get).toHaveBeenCalledWith('/users/me');
        expect(result).toEqual(mockResponse);
      });
    });

    describe('updateProfile', () => {
      it('should PATCH user profile with payload', async () => {
        const mockResponse = { data: { id: 'user-123', name: 'John Updated' } };
        mockApi.patch.mockResolvedValue(mockResponse);

        const payload = { name: 'John Updated' };
        const result = await authApi.updateProfile(payload);

        expect(mockApi.patch).toHaveBeenCalledWith('/users/me', payload);
        expect(result).toEqual(mockResponse);
      });
    });

    describe('deleteProfile', () => {
      it('should DELETE user profile', async () => {
        const mockResponse = { data: undefined };
        mockApi.delete.mockResolvedValue(mockResponse);

        const result = await authApi.deleteProfile();

        expect(mockApi.delete).toHaveBeenCalledWith('/users/me');
        expect(result).toEqual(mockResponse);
      });
    });
  });

  describe('menu.ts', () => {
    describe('fetchMenu', () => {
      it('should GET menu for pizzeria', async () => {
        const mockResponse = {
          data: {
            sections: [],
            pizzaCustomisations: [],
          },
        };
        mockApi.get.mockResolvedValue(mockResponse);

        const result = await menuApi.fetchMenu('kingspizza');

        expect(mockApi.get).toHaveBeenCalledWith('/pizzerias/kingspizza/menu');
        expect(result).toEqual(mockResponse);
      });
    });
  });

  describe('pizzas.ts', () => {
    describe('fetchPizzas', () => {
      it('should GET all pizzas for pizzeria', async () => {
        const mockResponse = {
          data: [
            { id: 'pizza-1', dishNumber: 1, nameKey: 'pizza.margherita' },
          ],
        };
        mockApi.get.mockResolvedValue(mockResponse);

        const result = await pizzasApi.fetchPizzas('kingspizza');

        expect(mockApi.get).toHaveBeenCalledWith('/pizzerias/kingspizza/pizzas');
        expect(result).toEqual(mockResponse);
      });
    });

    describe('fetchPizza', () => {
      it('should GET single pizza by ID', async () => {
        const mockResponse = {
          data: {
            id: 'pizza-1',
            dishNumber: 1,
            nameKey: 'pizza.margherita',
            descriptionKey: 'pizza.margherita.desc',
          },
        };
        mockApi.get.mockResolvedValue(mockResponse);

        const result = await pizzasApi.fetchPizza('kingspizza', 'pizza-1');

        expect(mockApi.get).toHaveBeenCalledWith('/pizzerias/kingspizza/pizzas/pizza-1');
        expect(result).toEqual(mockResponse);
      });
    });

    describe('checkSuitability', () => {
      it('should POST suitability check request', async () => {
        const mockResponse = {
          data: { suitable: true, violations: [], suggestions: [] },
        };
        mockApi.post.mockResolvedValue(mockResponse);

        const payload = { pizzaId: 'pizza-1', additionalIngredientIds: null, removedIngredientIds: null };
        const result = await pizzasApi.checkSuitability(payload);

        expect(mockApi.post).toHaveBeenCalledWith('/pizzas/suitability', payload);
        expect(result).toEqual(mockResponse);
      });
    });
  });

  describe('preferences.ts', () => {
    describe('fetchDiet', () => {
      it('should GET user diet preference', async () => {
        const mockResponse = { data: { diet: 'VEGETARIAN' } };
        mockApi.get.mockResolvedValue(mockResponse);

        const result = await preferencesApi.fetchDiet();

        expect(mockApi.get).toHaveBeenCalledWith('/users/me/diet');
        expect(result).toEqual(mockResponse);
      });
    });

    describe('updateDiet', () => {
      it('should PUT user diet preference', async () => {
        const mockResponse = { data: { diet: 'VEGAN' } };
        mockApi.put.mockResolvedValue(mockResponse);

        const payload = { diet: 'VEGAN' as const };
        const result = await preferencesApi.updateDiet(payload);

        expect(mockApi.put).toHaveBeenCalledWith('/users/me/diet', payload);
        expect(result).toEqual(mockResponse);
      });
    });

    describe('fetchPreferredIngredients', () => {
      it('should GET user preferred ingredients', async () => {
        const mockResponse = {
          data: [{ ingredientId: 'ing-1' }, { ingredientId: 'ing-2' }],
        };
        mockApi.get.mockResolvedValue(mockResponse);

        const result = await preferencesApi.fetchPreferredIngredients();

        expect(mockApi.get).toHaveBeenCalledWith('/users/me/preferences/ingredients/preferred');
        expect(result).toEqual(mockResponse);
      });
    });

    describe('addPreferredIngredient', () => {
      it('should POST new preferred ingredient', async () => {
        const mockResponse = { data: undefined };
        mockApi.post.mockResolvedValue(mockResponse);

        const payload = { ingredientId: 'ing-1' };
        const result = await preferencesApi.addPreferredIngredient(payload);

        expect(mockApi.post).toHaveBeenCalledWith(
          '/users/me/preferences/ingredients/preferred',
          payload
        );
        expect(result).toEqual(mockResponse);
      });
    });

    describe('removePreferredIngredient', () => {
      it('should DELETE preferred ingredient by ID', async () => {
        const mockResponse = { data: undefined };
        mockApi.delete.mockResolvedValue(mockResponse);

        const result = await preferencesApi.removePreferredIngredient('ing-1');

        expect(mockApi.delete).toHaveBeenCalledWith(
          '/users/me/preferences/ingredients/preferred/ing-1'
        );
        expect(result).toEqual(mockResponse);
      });
    });
  });

  describe('feedback.ts', () => {
    describe('submitServiceFeedback', () => {
      it('should POST service feedback', async () => {
        const mockResponse = {
          data: {
            id: 'feedback-1',
            userId: 'user-123',
            type: 'SERVICE',
            message: 'Great service!',
            rating: 5,
            createdAt: '2024-01-01',
          },
        };
        mockApi.post.mockResolvedValue(mockResponse);

        const payload = { message: 'Great service!', rating: 5, category: null };
        const result = await feedbackApi.submitServiceFeedback(payload);

        expect(mockApi.post).toHaveBeenCalledWith('/feedback/service', payload);
        expect(result).toEqual(mockResponse);
      });
    });
  });

  describe('scores.ts', () => {
    describe('createScore', () => {
      it('should POST new pizza score', async () => {
        const mockResponse = {
          data: {
            id: 'score-1',
            userId: 'user-123',
            pizzaId: 'pizza-1',
            pizzaType: 'TEMPLATE',
            score: 5,
            comment: 'Delicious!',
            createdAt: '2024-01-01',
          },
        };
        mockApi.post.mockResolvedValue(mockResponse);

        const payload = {
          pizzaId: 'pizza-1',
          pizzaType: 'TEMPLATE' as const,
          score: 5,
          comment: 'Delicious!',
        };
        const result = await scoresApi.createScore(payload);

        expect(mockApi.post).toHaveBeenCalledWith('/pizza-scores', payload);
        expect(result).toEqual(mockResponse);
      });
    });

    describe('fetchMyScores', () => {
      it('should GET user pizza scores', async () => {
        const mockResponse = {
          data: [
            { id: 'score-1', pizzaId: 'pizza-1', score: 5 },
            { id: 'score-2', pizzaId: 'pizza-2', score: 4 },
          ],
        };
        mockApi.get.mockResolvedValue(mockResponse);

        const result = await scoresApi.fetchMyScores();

        expect(mockApi.get).toHaveBeenCalledWith('/pizza-scores/me');
        expect(result).toEqual(mockResponse);
      });
    });
  });
});

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import { AuthProvider, AuthContext } from './AuthProvider';
import { useContext } from 'react';
import * as authApi from '../../api/auth';
import * as clientModule from '../../api/client';

// Mock the API modules
vi.mock('../../api/auth', () => ({
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  fetchProfile: vi.fn(),
  deleteProfile: vi.fn(),
}));

vi.mock('../../api/client', () => ({
  setAuthToken: vi.fn(),
  getAuthToken: vi.fn(),
}));

const mockAuthApi = vi.mocked(authApi);
const mockClientModule = vi.mocked(clientModule);

// Test component to access auth context
const TestConsumer = () => {
  const context = useContext(AuthContext);
  if (!context) return <div>No context</div>;

  return (
    <div>
      <span data-testid="authenticated">{String(context.isAuthenticated)}</span>
      <span data-testid="loading">{String(context.isLoading)}</span>
      <span data-testid="profile">{context.profile?.name ?? 'none'}</span>
      <button onClick={() => context.login('testpizza', { email: 'test@test.com', password: 'pass' })}>
        Login
      </button>
      <button onClick={() => context.logout()}>Logout</button>
      <button onClick={() => context.register('testpizza', { name: 'Test', email: 'test@test.com', password: 'pass' })}>
        Register
      </button>
      <button onClick={() => context.deleteAccount()}>Delete</button>
    </div>
  );
};

describe('AuthProvider', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    mockClientModule.getAuthToken.mockReturnValue(null);
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('initialization', () => {
    it('should initialize with loading state then become ready', async () => {
      render(
        <AuthProvider pizzeriaCode="testpizza">
          <TestConsumer />
        </AuthProvider>
      );

      // Should finish loading
      await waitFor(() => {
        expect(screen.getByTestId('loading').textContent).toBe('false');
      });

      expect(screen.getByTestId('authenticated').textContent).toBe('false');
      expect(screen.getByTestId('profile').textContent).toBe('none');
    });

    it('should load token from localStorage and fetch profile on mount', async () => {
      localStorage.setItem('pizzeria-testpizza-auth-token', 'stored-token');

      mockAuthApi.fetchProfile.mockResolvedValue({
        data: {
          id: 'user-1',
          name: 'Stored User',
          email: 'stored@test.com',
          emailVerified: true,
          preferredDiet: 'NONE',
          preferredIngredientIds: [],
          pizzeriaAdmin: null,
          createdAt: '2024-01-01',
          updatedAt: '2024-01-01',
        },
      } as never);

      render(
        <AuthProvider pizzeriaCode="testpizza">
          <TestConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('loading').textContent).toBe('false');
      });

      expect(mockClientModule.setAuthToken).toHaveBeenCalledWith('stored-token');
      expect(mockAuthApi.fetchProfile).toHaveBeenCalled();
      expect(screen.getByTestId('authenticated').textContent).toBe('true');
      expect(screen.getByTestId('profile').textContent).toBe('Stored User');
    });

    it('should clear auth if fetching profile fails on init', async () => {
      localStorage.setItem('pizzeria-testpizza-auth-token', 'invalid-token');
      mockAuthApi.fetchProfile.mockRejectedValue(new Error('Unauthorized'));

      render(
        <AuthProvider pizzeriaCode="testpizza">
          <TestConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('loading').textContent).toBe('false');
      });

      expect(screen.getByTestId('authenticated').textContent).toBe('false');
      expect(mockClientModule.setAuthToken).toHaveBeenCalledWith(null);
    });

    it('should not fetch profile if no pizzeriaCode provided', async () => {
      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('loading').textContent).toBe('false');
      });

      expect(mockAuthApi.fetchProfile).not.toHaveBeenCalled();
    });
  });

  describe('login', () => {
    it('should login successfully and store token', async () => {
      mockAuthApi.login.mockResolvedValue({
        data: { accessToken: 'new-token' },
      } as never);

      mockAuthApi.fetchProfile.mockResolvedValue({
        data: {
          id: 'user-1',
          name: 'Logged User',
          email: 'logged@test.com',
          emailVerified: true,
          preferredDiet: 'NONE',
          preferredIngredientIds: [],
          pizzeriaAdmin: null,
          createdAt: '2024-01-01',
          updatedAt: '2024-01-01',
        },
      } as never);

      render(
        <AuthProvider pizzeriaCode="testpizza">
          <TestConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('loading').textContent).toBe('false');
      });

      await act(async () => {
        screen.getByText('Login').click();
      });

      await waitFor(() => {
        expect(screen.getByTestId('authenticated').textContent).toBe('true');
      });

      expect(mockAuthApi.login).toHaveBeenCalledWith('testpizza', {
        email: 'test@test.com',
        password: 'pass',
      });
      expect(mockClientModule.setAuthToken).toHaveBeenCalledWith('new-token');
      expect(localStorage.getItem('pizzeria-testpizza-auth-token')).toBe('new-token');
      expect(screen.getByTestId('profile').textContent).toBe('Logged User');
    });
  });

  describe('register', () => {
    it('should register and return verification token', async () => {
      mockAuthApi.register.mockResolvedValue({
        data: {
          userId: 'new-user',
          emailVerified: false,
          verificationToken: 'verify-token-123',
        },
      } as never);

      let registerResult: { verificationToken: string } | undefined;

      const TestRegister = () => {
        const context = useContext(AuthContext);
        return (
          <button
            onClick={async () => {
              if (context) {
                registerResult = await context.register('testpizza', {
                  name: 'New User',
                  email: 'new@test.com',
                  password: 'password123',
                });
              }
            }}
          >
            Register
          </button>
        );
      };

      render(
        <AuthProvider pizzeriaCode="testpizza">
          <TestRegister />
        </AuthProvider>
      );

      await act(async () => {
        screen.getByText('Register').click();
      });

      await waitFor(() => {
        expect(registerResult).toEqual({ verificationToken: 'verify-token-123' });
      });

      expect(mockAuthApi.register).toHaveBeenCalledWith('testpizza', {
        name: 'New User',
        email: 'new@test.com',
        password: 'password123',
      });
    });
  });

  describe('logout', () => {
    it('should logout and clear auth state', async () => {
      localStorage.setItem('pizzeria-testpizza-auth-token', 'existing-token');
      mockClientModule.getAuthToken.mockReturnValue('existing-token');

      mockAuthApi.fetchProfile.mockResolvedValue({
        data: {
          id: 'user-1',
          name: 'Existing User',
          email: 'existing@test.com',
          emailVerified: true,
          preferredDiet: 'NONE',
          preferredIngredientIds: [],
          pizzeriaAdmin: null,
          createdAt: '2024-01-01',
          updatedAt: '2024-01-01',
        },
      } as never);

      mockAuthApi.logout.mockResolvedValue({} as never);

      render(
        <AuthProvider pizzeriaCode="testpizza">
          <TestConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('authenticated').textContent).toBe('true');
      });

      await act(async () => {
        screen.getByText('Logout').click();
      });

      await waitFor(() => {
        expect(screen.getByTestId('authenticated').textContent).toBe('false');
      });

      expect(mockAuthApi.logout).toHaveBeenCalled();
      expect(mockClientModule.setAuthToken).toHaveBeenCalledWith(null);
      expect(localStorage.getItem('pizzeria-testpizza-auth-token')).toBeNull();
    });

    it('should clear auth even if logout API fails', async () => {
      localStorage.setItem('pizzeria-testpizza-auth-token', 'existing-token');
      mockClientModule.getAuthToken.mockReturnValue('existing-token');

      mockAuthApi.fetchProfile.mockResolvedValue({
        data: {
          id: 'user-1',
          name: 'User',
          email: 'user@test.com',
          emailVerified: true,
          preferredDiet: 'NONE',
          preferredIngredientIds: [],
          pizzeriaAdmin: null,
          createdAt: '2024-01-01',
          updatedAt: '2024-01-01',
        },
      } as never);

      mockAuthApi.logout.mockRejectedValue(new Error('Network error'));

      // Component that catches the error from logout
      const TestLogoutWithError = () => {
        const context = useContext(AuthContext);
        return (
          <div>
            <span data-testid="auth">{String(context?.isAuthenticated)}</span>
            <button
              onClick={async () => {
                try {
                  await context?.logout();
                } catch {
                  // Expected to fail
                }
              }}
            >
              Logout
            </button>
          </div>
        );
      };

      render(
        <AuthProvider pizzeriaCode="testpizza">
          <TestLogoutWithError />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('auth').textContent).toBe('true');
      });

      await act(async () => {
        screen.getByText('Logout').click();
      });

      await waitFor(() => {
        expect(screen.getByTestId('auth').textContent).toBe('false');
      });

      // Auth should still be cleared even though API failed
      expect(mockClientModule.setAuthToken).toHaveBeenCalledWith(null);
    });
  });

  describe('deleteAccount', () => {
    it('should delete account and clear auth', async () => {
      localStorage.setItem('pizzeria-testpizza-auth-token', 'token');
      mockClientModule.getAuthToken.mockReturnValue('token');

      mockAuthApi.fetchProfile.mockResolvedValue({
        data: {
          id: 'user-1',
          name: 'User',
          email: 'user@test.com',
          emailVerified: true,
          preferredDiet: 'NONE',
          preferredIngredientIds: [],
          pizzeriaAdmin: null,
          createdAt: '2024-01-01',
          updatedAt: '2024-01-01',
        },
      } as never);

      mockAuthApi.deleteProfile.mockResolvedValue({} as never);

      render(
        <AuthProvider pizzeriaCode="testpizza">
          <TestConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('authenticated').textContent).toBe('true');
      });

      await act(async () => {
        screen.getByText('Delete').click();
      });

      await waitFor(() => {
        expect(screen.getByTestId('authenticated').textContent).toBe('false');
      });

      expect(mockAuthApi.deleteProfile).toHaveBeenCalled();
      expect(mockClientModule.setAuthToken).toHaveBeenCalledWith(null);
    });
  });

  describe('unauthorized event handling', () => {
    it('should clear auth when auth:unauthorized event is dispatched', async () => {
      localStorage.setItem('pizzeria-testpizza-auth-token', 'token');
      mockClientModule.getAuthToken.mockReturnValue('token');

      mockAuthApi.fetchProfile.mockResolvedValue({
        data: {
          id: 'user-1',
          name: 'User',
          email: 'user@test.com',
          emailVerified: true,
          preferredDiet: 'NONE',
          preferredIngredientIds: [],
          pizzeriaAdmin: null,
          createdAt: '2024-01-01',
          updatedAt: '2024-01-01',
        },
      } as never);

      render(
        <AuthProvider pizzeriaCode="testpizza">
          <TestConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('authenticated').textContent).toBe('true');
      });

      // Dispatch unauthorized event
      act(() => {
        window.dispatchEvent(new CustomEvent('auth:unauthorized'));
      });

      await waitFor(() => {
        expect(screen.getByTestId('authenticated').textContent).toBe('false');
      });

      expect(mockClientModule.setAuthToken).toHaveBeenCalledWith(null);
    });
  });
});

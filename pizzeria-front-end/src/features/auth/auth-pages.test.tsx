import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { I18nextProvider } from 'react-i18next';
import { LoginPage } from './LoginPage';
import { RegisterPage } from './RegisterPage';
import { VerifyEmailPage } from './VerifyEmailPage';
import { ForgotPasswordPage } from './ForgotPasswordPage';
import { ResetPasswordPage } from './ResetPasswordPage';
import { AuthContext, type AuthContextValue } from './AuthProvider';
import { PizzeriaProvider } from '../../routes/PizzeriaProvider';
import { testI18n } from '../../tests/test-utils';
import * as authApi from '../../api/auth';
import * as clientModule from '../../api/client';

// Mock APIs
vi.mock('../../api/auth', () => ({
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  fetchProfile: vi.fn(),
  verifyEmail: vi.fn(),
  forgotPassword: vi.fn(),
  resetPassword: vi.fn(),
}));

vi.mock('../../api/client', () => ({
  setAuthToken: vi.fn(),
  getAuthToken: vi.fn(() => null),
  getApiErrorMessage: vi.fn((err: unknown) => {
    if (err instanceof Error) return err.message;
    return 'An error occurred';
  }),
}));

const mockAuthApi = vi.mocked(authApi);
const mockClientModule = vi.mocked(clientModule);

const createQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

// Mock auth context for pages
const createMockAuthContext = (overrides: Partial<AuthContextValue> = {}): AuthContextValue => ({
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

describe('LoginPage', () => {
  const renderLoginPage = (
    mockAuth: AuthContextValue = createMockAuthContext(),
    initialRoute = '/testpizza/login'
  ) => {
    const queryClient = createQueryClient();

    return render(
      <QueryClientProvider client={queryClient}>
        <I18nextProvider i18n={testI18n}>
          <MemoryRouter
            initialEntries={[initialRoute]}
            future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
          >
            <Routes>
              <Route path="/:pizzeriaCode/*" element={<PizzeriaProvider />}>
                <Route
                  path="login"
                  element={
                    <AuthContext.Provider value={mockAuth}>
                      <LoginPage />
                    </AuthContext.Provider>
                  }
                />
                <Route path="" element={<div>Home Page</div>} />
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

  it('should render login form', () => {
    renderLoginPage();

    expect(screen.getByRole('heading', { name: /sign in/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
  });

  it('should have link to register page', () => {
    renderLoginPage();

    expect(screen.getByRole('link', { name: /register here/i })).toHaveAttribute(
      'href',
      '/testpizza/register'
    );
  });

  it('should have link to forgot password page', () => {
    renderLoginPage();

    expect(screen.getByRole('link', { name: /forgot your password/i })).toHaveAttribute(
      'href',
      '/testpizza/forgot-password'
    );
  });

  it('should update form fields on input', async () => {
    const user = userEvent.setup();
    renderLoginPage();

    const emailInput = screen.getByLabelText(/email/i);
    const passwordInput = screen.getByLabelText(/password/i);

    await user.type(emailInput, 'test@example.com');
    await user.type(passwordInput, 'password123');

    expect(emailInput).toHaveValue('test@example.com');
    expect(passwordInput).toHaveValue('password123');
  });

  it('should call login on form submit', async () => {
    const user = userEvent.setup();
    const mockLogin = vi.fn().mockResolvedValue(undefined);
    const mockAuth = createMockAuthContext({ login: mockLogin });

    renderLoginPage(mockAuth);

    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith('testpizza', {
        email: 'test@example.com',
        password: 'password123',
      });
    });
  });

  it('should show loading state during submission', async () => {
    const user = userEvent.setup();
    const mockLogin = vi.fn().mockImplementation(
      () => new Promise((resolve) => setTimeout(resolve, 100))
    );
    const mockAuth = createMockAuthContext({ login: mockLogin });

    renderLoginPage(mockAuth);

    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(screen.getByRole('button', { name: /signing in/i })).toBeDisabled();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /sign in/i })).not.toBeDisabled();
    });
  });

  it('should display error message on login failure', async () => {
    const user = userEvent.setup();
    const mockLogin = vi.fn().mockRejectedValue(new Error('Invalid credentials'));
    const mockAuth = createMockAuthContext({ login: mockLogin });
    mockClientModule.getApiErrorMessage.mockReturnValue('Invalid credentials');

    renderLoginPage(mockAuth);

    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.type(screen.getByLabelText(/password/i), 'wrong');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Invalid credentials');
    });
  });

  it('should dismiss error when close button is clicked', async () => {
    const user = userEvent.setup();
    const mockLogin = vi.fn().mockRejectedValue(new Error('Error'));
    const mockAuth = createMockAuthContext({ login: mockLogin });
    mockClientModule.getApiErrorMessage.mockReturnValue('Error');

    renderLoginPage(mockAuth);

    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.type(screen.getByLabelText(/password/i), 'wrong');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /dismiss/i }));

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});

describe('RegisterPage', () => {
  const renderRegisterPage = (
    mockAuth: AuthContextValue = createMockAuthContext()
  ) => {
    const queryClient = createQueryClient();

    return render(
      <QueryClientProvider client={queryClient}>
        <I18nextProvider i18n={testI18n}>
          <MemoryRouter
            initialEntries={['/testpizza/register']}
            future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
          >
            <Routes>
              <Route path="/:pizzeriaCode/*" element={<PizzeriaProvider />}>
                <Route
                  path="register"
                  element={
                    <AuthContext.Provider value={mockAuth}>
                      <RegisterPage />
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

  it('should render registration form', () => {
    renderRegisterPage();

    expect(screen.getByRole('heading', { name: /create account/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/full name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /create account/i })).toBeInTheDocument();
  });

  it('should have link to login page', () => {
    renderRegisterPage();

    expect(screen.getByRole('link', { name: /sign in here/i })).toHaveAttribute(
      'href',
      '/testpizza/login'
    );
  });

  it('should update form fields on input', async () => {
    const user = userEvent.setup();
    renderRegisterPage();

    const nameInput = screen.getByLabelText(/full name/i);
    const emailInput = screen.getByLabelText(/email/i);
    const passwordInput = screen.getByLabelText(/password/i);

    await user.type(nameInput, 'John Doe');
    await user.type(emailInput, 'john@example.com');
    await user.type(passwordInput, 'password123');

    expect(nameInput).toHaveValue('John Doe');
    expect(emailInput).toHaveValue('john@example.com');
    expect(passwordInput).toHaveValue('password123');
  });

  it('should call register on form submit', async () => {
    const user = userEvent.setup();
    const mockRegister = vi.fn().mockResolvedValue({ verificationToken: 'token123' });
    const mockAuth = createMockAuthContext({ register: mockRegister });

    renderRegisterPage(mockAuth);

    await user.type(screen.getByLabelText(/full name/i), 'John Doe');
    await user.type(screen.getByLabelText(/email/i), 'john@example.com');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /create account/i }));

    await waitFor(() => {
      expect(mockRegister).toHaveBeenCalledWith('testpizza', {
        name: 'John Doe',
        email: 'john@example.com',
        password: 'password123',
      });
    });
  });

  it('should show success message with verification token after registration', async () => {
    const user = userEvent.setup();
    const mockRegister = vi.fn().mockResolvedValue({ verificationToken: 'verify-token-xyz' });
    const mockAuth = createMockAuthContext({ register: mockRegister });

    renderRegisterPage(mockAuth);

    await user.type(screen.getByLabelText(/full name/i), 'John Doe');
    await user.type(screen.getByLabelText(/email/i), 'john@example.com');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /create account/i }));

    await waitFor(() => {
      expect(screen.getByText(/registration successful/i)).toBeInTheDocument();
    });

    // Token is displayed in the curl command
    expect(screen.getByText(/verify-token-xyz/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /verify email/i })).toHaveAttribute(
      'href',
      '/testpizza/verify-email'
    );
    expect(screen.getByRole('link', { name: /sign in/i })).toHaveAttribute(
      'href',
      '/testpizza/login'
    );
  });

  it('should show loading state during submission', async () => {
    const user = userEvent.setup();
    const mockRegister = vi.fn().mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve({ verificationToken: 'token' }), 100))
    );
    const mockAuth = createMockAuthContext({ register: mockRegister });

    renderRegisterPage(mockAuth);

    await user.type(screen.getByLabelText(/full name/i), 'John Doe');
    await user.type(screen.getByLabelText(/email/i), 'john@example.com');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /create account/i }));

    expect(screen.getByRole('button', { name: /creating account/i })).toBeDisabled();
  });

  it('should display error message on registration failure', async () => {
    const user = userEvent.setup();
    const mockRegister = vi.fn().mockRejectedValue(new Error('Email already exists'));
    const mockAuth = createMockAuthContext({ register: mockRegister });
    mockClientModule.getApiErrorMessage.mockReturnValue('Email already exists');

    renderRegisterPage(mockAuth);

    await user.type(screen.getByLabelText(/full name/i), 'John Doe');
    await user.type(screen.getByLabelText(/email/i), 'john@example.com');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /create account/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Email already exists');
    });
  });
});

describe('VerifyEmailPage', () => {
  const renderVerifyEmailPage = (initialRoute = '/testpizza/verify-email') => {
    const queryClient = createQueryClient();
    const mockAuth = createMockAuthContext();

    return render(
      <QueryClientProvider client={queryClient}>
        <I18nextProvider i18n={testI18n}>
          <MemoryRouter
            initialEntries={[initialRoute]}
            future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
          >
            <Routes>
              <Route path="/:pizzeriaCode/*" element={<PizzeriaProvider />}>
                <Route
                  path="verify-email"
                  element={
                    <AuthContext.Provider value={mockAuth}>
                      <VerifyEmailPage />
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

  it('should render verification form', () => {
    renderVerifyEmailPage();

    expect(screen.getByRole('heading', { name: /verify email/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/verification token/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /verify email/i })).toBeInTheDocument();
  });

  it('should have link to login page', () => {
    renderVerifyEmailPage();

    expect(screen.getByRole('link', { name: /sign in/i })).toHaveAttribute(
      'href',
      '/testpizza/login'
    );
  });

  it('should pre-fill token from URL query parameter', () => {
    renderVerifyEmailPage('/testpizza/verify-email?token=prefilledtoken');

    expect(screen.getByLabelText(/verification token/i)).toHaveValue('prefilledtoken');
  });

  it('should call verifyEmail API on form submit', async () => {
    const user = userEvent.setup();
    mockAuthApi.verifyEmail.mockResolvedValue({} as never);

    renderVerifyEmailPage();

    await user.type(screen.getByLabelText(/verification token/i), 'my-token');
    await user.click(screen.getByRole('button', { name: /verify email/i }));

    await waitFor(() => {
      expect(mockAuthApi.verifyEmail).toHaveBeenCalledWith('testpizza', { token: 'my-token' });
    });
  });

  it('should show success message after verification', async () => {
    const user = userEvent.setup();
    mockAuthApi.verifyEmail.mockResolvedValue({} as never);

    renderVerifyEmailPage();

    await user.type(screen.getByLabelText(/verification token/i), 'valid-token');
    await user.click(screen.getByRole('button', { name: /verify email/i }));

    await waitFor(() => {
      expect(screen.getByText(/email verified/i)).toBeInTheDocument();
    });

    // Should show login link
    expect(screen.getByRole('link', { name: /sign in/i })).toHaveAttribute(
      'href',
      '/testpizza/login'
    );
  });

  it('should show loading state during submission', async () => {
    const user = userEvent.setup();
    mockAuthApi.verifyEmail.mockImplementation(
      () => new Promise((resolve) => setTimeout(resolve, 100)) as never
    );

    renderVerifyEmailPage();

    await user.type(screen.getByLabelText(/verification token/i), 'token');
    await user.click(screen.getByRole('button', { name: /verify email/i }));

    expect(screen.getByRole('button', { name: /verifying/i })).toBeDisabled();
  });

  it('should display error message on verification failure', async () => {
    const user = userEvent.setup();
    mockAuthApi.verifyEmail.mockRejectedValue(new Error('Invalid token'));
    mockClientModule.getApiErrorMessage.mockReturnValue('Invalid token');

    renderVerifyEmailPage();

    await user.type(screen.getByLabelText(/verification token/i), 'invalid');
    await user.click(screen.getByRole('button', { name: /verify email/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Invalid token');
    });
  });

  it('should dismiss error when close button is clicked', async () => {
    const user = userEvent.setup();
    mockAuthApi.verifyEmail.mockRejectedValue(new Error('Error'));
    mockClientModule.getApiErrorMessage.mockReturnValue('Error');

    renderVerifyEmailPage();

    await user.type(screen.getByLabelText(/verification token/i), 'invalid');
    await user.click(screen.getByRole('button', { name: /verify email/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /dismiss/i }));

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});

describe('ForgotPasswordPage', () => {
  const renderForgotPasswordPage = () => {
    const queryClient = createQueryClient();
    const mockAuth = createMockAuthContext();

    return render(
      <QueryClientProvider client={queryClient}>
        <I18nextProvider i18n={testI18n}>
          <MemoryRouter
            initialEntries={['/testpizza/forgot-password']}
            future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
          >
            <Routes>
              <Route path="/:pizzeriaCode/*" element={<PizzeriaProvider />}>
                <Route
                  path="forgot-password"
                  element={
                    <AuthContext.Provider value={mockAuth}>
                      <ForgotPasswordPage />
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

  it('should render forgot password form', () => {
    renderForgotPasswordPage();

    expect(screen.getByRole('heading', { name: /forgot password/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /send reset token/i })).toBeInTheDocument();
  });

  it('should call forgotPassword API on form submit', async () => {
    const user = userEvent.setup();
    mockAuthApi.forgotPassword.mockResolvedValue({ data: { resetToken: 'reset-token-123' } } as never);

    renderForgotPasswordPage();

    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.click(screen.getByRole('button', { name: /send reset token/i }));

    await waitFor(() => {
      expect(mockAuthApi.forgotPassword).toHaveBeenCalledWith('testpizza', {
        email: 'test@example.com',
      });
    });
  });

  it('should show success state with reset token', async () => {
    const user = userEvent.setup();
    mockAuthApi.forgotPassword.mockResolvedValue({ data: { resetToken: 'reset-token-abc' } } as never);

    renderForgotPasswordPage();

    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.click(screen.getByRole('button', { name: /send reset token/i }));

    await waitFor(() => {
      expect(screen.getByText(/reset token sent/i)).toBeInTheDocument();
    });

    expect(screen.getByText(/reset-token-abc/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /reset password/i })).toHaveAttribute(
      'href',
      '/testpizza/reset-password'
    );
  });

  it('should show loading state during submission', async () => {
    const user = userEvent.setup();
    mockAuthApi.forgotPassword.mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve({ data: { resetToken: 'token' } }), 100)) as never
    );

    renderForgotPasswordPage();

    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.click(screen.getByRole('button', { name: /send reset token/i }));

    expect(screen.getByRole('button', { name: /sending/i })).toBeDisabled();
  });

  it('should display error message on failure', async () => {
    const user = userEvent.setup();
    mockAuthApi.forgotPassword.mockRejectedValue(new Error('User not found'));
    mockClientModule.getApiErrorMessage.mockReturnValue('User not found');

    renderForgotPasswordPage();

    await user.type(screen.getByLabelText(/email/i), 'unknown@example.com');
    await user.click(screen.getByRole('button', { name: /send reset token/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('User not found');
    });
  });
});

describe('ResetPasswordPage', () => {
  const renderResetPasswordPage = (initialRoute = '/testpizza/reset-password') => {
    const queryClient = createQueryClient();
    const mockAuth = createMockAuthContext();

    return render(
      <QueryClientProvider client={queryClient}>
        <I18nextProvider i18n={testI18n}>
          <MemoryRouter
            initialEntries={[initialRoute]}
            future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
          >
            <Routes>
              <Route path="/:pizzeriaCode/*" element={<PizzeriaProvider />}>
                <Route
                  path="reset-password"
                  element={
                    <AuthContext.Provider value={mockAuth}>
                      <ResetPasswordPage />
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

  it('should render reset password form', () => {
    renderResetPasswordPage();

    expect(screen.getByRole('heading', { name: /reset password/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/reset token/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/new password/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/confirm password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /reset password/i })).toBeInTheDocument();
  });

  it('should pre-fill token from URL query parameter', () => {
    renderResetPasswordPage('/testpizza/reset-password?token=prefilled-reset-token');

    expect(screen.getByLabelText(/reset token/i)).toHaveValue('prefilled-reset-token');
  });

  it('should show error when passwords do not match', async () => {
    const user = userEvent.setup();

    renderResetPasswordPage();

    await user.type(screen.getByLabelText(/reset token/i), 'some-token');
    await user.type(screen.getByLabelText(/new password/i), 'NewPassword123!');
    await user.type(screen.getByLabelText(/confirm password/i), 'DifferentPassword!');
    await user.click(screen.getByRole('button', { name: /reset password/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/passwords do not match/i);
    });
  });

  it('should call resetPassword API on form submit', async () => {
    const user = userEvent.setup();
    mockAuthApi.resetPassword.mockResolvedValue({} as never);

    renderResetPasswordPage();

    await user.type(screen.getByLabelText(/reset token/i), 'valid-token');
    await user.type(screen.getByLabelText(/new password/i), 'NewPassword123!');
    await user.type(screen.getByLabelText(/confirm password/i), 'NewPassword123!');
    await user.click(screen.getByRole('button', { name: /reset password/i }));

    await waitFor(() => {
      expect(mockAuthApi.resetPassword).toHaveBeenCalledWith('testpizza', {
        token: 'valid-token',
        newPassword: 'NewPassword123!',
      });
    });
  });

  it('should show success message after reset', async () => {
    const user = userEvent.setup();
    mockAuthApi.resetPassword.mockResolvedValue({} as never);

    renderResetPasswordPage();

    await user.type(screen.getByLabelText(/reset token/i), 'valid-token');
    await user.type(screen.getByLabelText(/new password/i), 'NewPassword123!');
    await user.type(screen.getByLabelText(/confirm password/i), 'NewPassword123!');
    await user.click(screen.getByRole('button', { name: /reset password/i }));

    await waitFor(() => {
      expect(screen.getByText(/password reset!/i)).toBeInTheDocument();
    });

    expect(screen.getByRole('link', { name: /sign in/i })).toHaveAttribute(
      'href',
      '/testpizza/login'
    );
  });

  it('should show loading state during submission', async () => {
    const user = userEvent.setup();
    mockAuthApi.resetPassword.mockImplementation(
      () => new Promise((resolve) => setTimeout(resolve, 100)) as never
    );

    renderResetPasswordPage();

    await user.type(screen.getByLabelText(/reset token/i), 'token');
    await user.type(screen.getByLabelText(/new password/i), 'NewPassword123!');
    await user.type(screen.getByLabelText(/confirm password/i), 'NewPassword123!');
    await user.click(screen.getByRole('button', { name: /reset password/i }));

    expect(screen.getByRole('button', { name: /resetting/i })).toBeDisabled();
  });

  it('should display error message on reset failure', async () => {
    const user = userEvent.setup();
    mockAuthApi.resetPassword.mockRejectedValue(new Error('Invalid token'));
    mockClientModule.getApiErrorMessage.mockReturnValue('Invalid token');

    renderResetPasswordPage();

    await user.type(screen.getByLabelText(/reset token/i), 'bad-token');
    await user.type(screen.getByLabelText(/new password/i), 'NewPassword123!');
    await user.type(screen.getByLabelText(/confirm password/i), 'NewPassword123!');
    await user.click(screen.getByRole('button', { name: /reset password/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Invalid token');
    });
  });
});

import { type ReactElement, type ReactNode } from 'react';
import { render, type RenderOptions } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { I18nextProvider } from 'react-i18next';
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import { AuthContext } from '../features/auth/AuthProvider';
import { ConsoleCaptureContext } from '../hooks/useConsoleCapture';
import type { ConsoleCaptureContextValue } from '../hooks/useConsoleCapture';
import type {
  UserProfileResponse,
  UserLoginRequest,
  UserRegisterRequest,
} from '../types/api';

// Default mock console capture context
const defaultConsoleCaptureContext: ConsoleCaptureContextValue = {
  levelSettings: { error: true, warn: true, info: false, debug: false },
  setLevelEnabled: () => {},
  openPanel: () => {},
  hasLogs: false,
  errorCount: 0,
  warnCount: 0,
};

// Create a test i18n instance
const testI18n = i18n.createInstance();
void testI18n.use(initReactI18next).init({
  lng: 'en',
  fallbackLng: 'en',
  supportedLngs: ['en', 'sv'],
  defaultNS: 'common',
  resources: {
    en: {
      common: {
        'nav.home': 'Home',
        'nav.menu': 'Menu',
        'nav.pizzas': 'Pizzas',
        'nav.profile': 'Profile',
        'nav.preferences': 'Preferences',
        'nav.scores': 'Scores',
        'nav.feedback': 'Feedback',
        'nav.signIn': 'Sign In',
        'nav.signOut': 'Sign Out',
        'status.loading': 'Loading...',
        'status.error': 'Error',
        'status.notFound': 'Not Found',
        'currency.sek': 'SEK',
        'actions.back': 'Back',
        'actions.save': 'Save',
        'actions.cancel': 'Cancel',
        'actions.delete': 'Delete',
        'actions.edit': 'Edit',
        'actions.close': 'Close',
        'home.welcome': 'Welcome to',
        'home.tagline': 'Your favorite neighborhood pizzeria',
        'home.cta': 'View Our Menu',
        'console.title': 'Console Logs',
        'console.settings': 'Console settings',
        'console.captureSettings': 'Console Capture',
        'console.viewLogs': 'View captured logs',
        'console.noLogs': 'No console logs yet',
        'console.copyLatest': 'Copy Latest',
        'console.copied': 'Copied!',
        'console.clearAll': 'Clear All',
        'console.level.error': 'Errors',
        'console.level.warn': 'Warnings',
        'console.level.info': 'Info',
        'console.level.debug': 'Debug',
      },
      auth: {
        'login.title': 'Sign In',
        'login.subtitle': 'Welcome back! Please sign in to your account.',
        'login.email': 'Email',
        'login.emailPlaceholder': 'Enter your email',
        'login.password': 'Password',
        'login.passwordPlaceholder': 'Enter your password',
        'login.submit': 'Sign In',
        'login.submitting': 'Signing in...',
        'login.noAccount': "Don't have an account?",
        'login.registerLink': 'Register here',
        'register.title': 'Create Account',
        'register.subtitle': 'Join us and start ordering your favorite pizzas!',
        'register.name': 'Full Name',
        'register.namePlaceholder': 'Enter your full name',
        'register.email': 'Email',
        'register.emailPlaceholder': 'Enter your email',
        'register.password': 'Password',
        'register.passwordPlaceholder': 'Create a password (min 8 characters)',
        'register.submit': 'Create Account',
        'register.submitting': 'Creating account...',
        'register.hasAccount': 'Already have an account?',
        'register.loginLink': 'Sign in here',
        'register.success.title': 'Registration Successful!',
        'register.success.message': 'Please check your email to verify your account.',
        'register.success.verificationToken': 'Verification token (for testing):',
        'verifyEmail.title': 'Verify Email',
        'verifyEmail.subtitle': 'Enter the verification token sent to your email.',
        'verifyEmail.token': 'Verification Token',
        'verifyEmail.tokenPlaceholder': 'Enter verification token',
        'verifyEmail.submit': 'Verify Email',
        'verifyEmail.submitting': 'Verifying...',
        'verifyEmail.success.title': 'Email Verified!',
        'verifyEmail.success.message': 'Your email has been verified. You can now sign in.',
        'login.forgotPassword': 'Forgot your password?',
        'forgotPassword.title': 'Forgot Password',
        'forgotPassword.subtitle': 'Enter your email and we\'ll send you a reset token.',
        'forgotPassword.email': 'Email',
        'forgotPassword.emailPlaceholder': 'Enter your email',
        'forgotPassword.submit': 'Send Reset Token',
        'forgotPassword.submitting': 'Sending...',
        'forgotPassword.success.title': 'Reset Token Sent',
        'forgotPassword.success.message': 'Use the token below to reset your password.',
        'forgotPassword.success.curlCommand': 'Reset password with curl (for testing):',
        'resetPassword.title': 'Reset Password',
        'resetPassword.subtitle': 'Enter the reset token and your new password.',
        'resetPassword.token': 'Reset Token',
        'resetPassword.tokenPlaceholder': 'Enter reset token',
        'resetPassword.newPassword': 'New Password',
        'resetPassword.newPasswordPlaceholder': 'Enter new password (min 8 characters)',
        'resetPassword.confirmPassword': 'Confirm Password',
        'resetPassword.confirmPasswordPlaceholder': 'Confirm new password',
        'resetPassword.submit': 'Reset Password',
        'resetPassword.submitting': 'Resetting...',
        'resetPassword.success.title': 'Password Reset!',
        'resetPassword.success.message': 'Your password has been reset. You can now sign in with your new password.',
        'resetPassword.error.passwordMismatch': 'Passwords do not match',
        'profile.title': 'My Profile',
        'profile.name': 'Name',
        'profile.email': 'Email',
        'profile.phone': 'Phone',
        'profile.phonePlaceholder': 'Enter your phone number',
        'profile.emailVerified': 'Email Verified',
        'profile.emailNotVerified': 'Email Not Verified',
        'profile.memberSince': 'Member since',
        'profile.lastUpdated': 'Last updated',
        'profile.updateSuccess': 'Profile updated successfully',
        'profile.deleteAccount.title': 'Delete Account',
        'profile.deleteAccount.warning': 'This action cannot be undone.',
        'profile.deleteAccount.confirm': 'Are you sure you want to delete your account?',
        'profile.deleteAccount.button': 'Delete My Account',
      },
      menu: {
        'title': 'Our Menu',
        'subtitle': 'Discover our delicious selection',
        'item.familySize': 'Family Size',
        'item.regularSize': 'Regular',
        'item.ingredients': 'Ingredients',
        'item.allergens': 'Allergens',
        'item.dietary.VEGAN': 'Vegan',
        'item.dietary.VEGETARIAN': 'Vegetarian',
        'item.dietary.CARNIVORE': 'Contains Meat',
        'customisations.title': 'Customisations',
        'customisations.subtitle': 'Add extras to your order',
        'suitability.title': 'Check Suitability',
        'suitability.subtitle': 'See if this pizza matches your dietary preferences',
        'suitability.check': 'Check Suitability',
        'suitability.checking': 'Checking...',
        'suitability.suitable': 'This pizza is suitable for your diet!',
        'suitability.notSuitable': 'This pizza may not be suitable for your diet',
        'suitability.violations': 'Dietary Violations',
        'suitability.suggestions': 'Suggestions',
        'suitability.loginRequired': 'Please sign in to check dietary suitability',
        'scores.title': 'My Pizza Ratings',
        'scores.subtitle': 'Rate your favorite pizzas',
        'scores.noScores': "You haven't rated any pizzas yet",
        'scores.addScore': 'Add Rating',
        'scores.pizza': 'Pizza',
        'scores.score': 'Score',
        'scores.comment': 'Comment',
        'scores.commentPlaceholder': 'Share your thoughts...',
        'scores.submit': 'Submit Rating',
        'scores.submitting': 'Submitting...',
        'scores.success': 'Rating submitted successfully',
        'scores.selectPizza': 'Select a pizza',
        'feedback.title': 'Service Feedback',
        'feedback.subtitle': "We'd love to hear from you",
        'feedback.message': 'Your Feedback',
        'feedback.messagePlaceholder': 'Tell us about your experience...',
        'feedback.rating': 'Rating (optional)',
        'feedback.category': 'Category (optional)',
        'feedback.categoryPlaceholder': 'e.g., Service, Delivery, Quality',
        'feedback.submit': 'Send Feedback',
        'feedback.submitting': 'Sending...',
        'feedback.success': 'Thank you for your feedback!',
        'preferences.title': 'My Preferences',
        'preferences.diet.title': 'Dietary Preference',
        'preferences.diet.subtitle': 'Select your dietary preference',
        'preferences.diet.VEGAN': 'Vegan',
        'preferences.diet.VEGETARIAN': 'Vegetarian',
        'preferences.diet.CARNIVORE': 'Carnivore',
        'preferences.diet.NONE': 'No Preference',
        'preferences.diet.updateSuccess': 'Dietary preference updated',
        'preferences.ingredients.title': 'Preferred Ingredients',
        'preferences.ingredients.subtitle': 'Select your favorite ingredients',
        'preferences.ingredients.noPreferences': 'No preferred ingredients selected',
        'preferences.ingredients.add': 'Add Ingredient',
        'preferences.ingredients.remove': 'Remove',
        'preferences.ingredients.addSuccess': 'Ingredient added to preferences',
        'preferences.ingredients.removeSuccess': 'Ingredient removed from preferences',
      },
    },
    sv: {
      common: {
        'nav.home': 'Hem',
        'nav.menu': 'Meny',
        'nav.pizzas': 'Pizzor',
        'nav.signIn': 'Logga in',
        'nav.signOut': 'Logga ut',
      },
    },
  },
  interpolation: {
    escapeValue: false,
  },
});

export { testI18n };

// Mock auth context value type
export interface MockAuthContextValue {
  isAuthenticated: boolean;
  isLoading: boolean;
  profile: UserProfileResponse | null;
  login: (pizzeriaCode: string, payload: UserLoginRequest) => Promise<void>;
  register: (
    pizzeriaCode: string,
    payload: UserRegisterRequest
  ) => Promise<{ verificationToken: string }>;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
  deleteAccount: () => Promise<void>;
}

// Create default mock auth context
export const createMockAuthContext = (
  overrides: Partial<MockAuthContextValue> = {}
): MockAuthContextValue => ({
  isAuthenticated: false,
  isLoading: false,
  profile: null,
  login: async () => {},
  register: async () => ({ verificationToken: 'test-token' }),
  logout: async () => {},
  refreshProfile: async () => {},
  deleteAccount: async () => {},
  ...overrides,
});

// Create authenticated mock auth context
export const createAuthenticatedMockAuthContext = (
  overrides: Partial<MockAuthContextValue> = {}
): MockAuthContextValue =>
  createMockAuthContext({
    isAuthenticated: true,
    profile: {
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
    },
    ...overrides,
  });

// Create a new QueryClient for each test
const createTestQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: 0,
        staleTime: 0,
      },
      mutations: {
        retry: false,
      },
    },
  });

// Pizzeria context for testing
import { createContext } from 'react';

interface PizzeriaContextValue {
  pizzeriaCode: string;
}

export const TestPizzeriaContext = createContext<PizzeriaContextValue | null>(
  null
);

// Provider options for renderWithProviders
interface ProviderOptions {
  pizzeriaCode?: string;
  authContext?: MockAuthContextValue;
  initialRoute?: string;
  queryClient?: QueryClient;
}

// All providers wrapper
interface AllProvidersProps {
  children: ReactNode;
  options: ProviderOptions;
}

const AllProviders = ({ children, options }: AllProvidersProps) => {
  const {
    pizzeriaCode = 'testpizzeria',
    authContext = createMockAuthContext(),
    initialRoute = '/',
    queryClient = createTestQueryClient(),
  } = options;

  return (
    <QueryClientProvider client={queryClient}>
      <I18nextProvider i18n={testI18n}>
        <ConsoleCaptureContext.Provider value={defaultConsoleCaptureContext}>
          <MemoryRouter
            initialEntries={[initialRoute]}
            future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
          >
            <TestPizzeriaContext.Provider value={{ pizzeriaCode }}>
              <AuthContext.Provider value={authContext}>
                {children}
              </AuthContext.Provider>
            </TestPizzeriaContext.Provider>
          </MemoryRouter>
        </ConsoleCaptureContext.Provider>
      </I18nextProvider>
    </QueryClientProvider>
  );
};

// Custom render function with all providers
interface CustomRenderOptions extends Omit<RenderOptions, 'wrapper'> {
  providerOptions?: ProviderOptions;
}

export const renderWithProviders = (
  ui: ReactElement,
  { providerOptions = {}, ...renderOptions }: CustomRenderOptions = {}
) => {
  const queryClient = providerOptions.queryClient ?? createTestQueryClient();

  return {
    ...render(ui, {
      wrapper: ({ children }) => (
        <AllProviders options={{ ...providerOptions, queryClient }}>
          {children}
        </AllProviders>
      ),
      ...renderOptions,
    }),
    queryClient,
  };
};

// Render with route - for testing components that need specific route params
interface RenderWithRouteOptions extends CustomRenderOptions {
  path: string;
  route: string;
}

export const renderWithRoute = (
  ui: ReactElement,
  { path, route, providerOptions = {}, ...renderOptions }: RenderWithRouteOptions
) => {
  const queryClient = providerOptions.queryClient ?? createTestQueryClient();
  const {
    pizzeriaCode = 'testpizzeria',
    authContext = createMockAuthContext(),
  } = providerOptions;

  return {
    ...render(
      <QueryClientProvider client={queryClient}>
        <I18nextProvider i18n={testI18n}>
          <ConsoleCaptureContext.Provider value={defaultConsoleCaptureContext}>
            <MemoryRouter
              initialEntries={[route]}
              future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
            >
              <TestPizzeriaContext.Provider value={{ pizzeriaCode }}>
                <AuthContext.Provider value={authContext}>
                  <Routes>
                    <Route path={path} element={ui} />
                  </Routes>
                </AuthContext.Provider>
              </TestPizzeriaContext.Provider>
            </MemoryRouter>
          </ConsoleCaptureContext.Provider>
        </I18nextProvider>
      </QueryClientProvider>,
      renderOptions
    ),
    queryClient,
  };
};

// Re-export everything from testing-library
export * from '@testing-library/react';
export { default as userEvent } from '@testing-library/user-event';

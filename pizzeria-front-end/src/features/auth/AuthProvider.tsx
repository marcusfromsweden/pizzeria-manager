import {
  createContext,
  useCallback,
  useEffect,
  useState,
  type ReactNode,
} from 'react';
import * as authApi from '../../api/auth';
import { setAuthToken, getAuthToken } from '../../api/client';
import type {
  UserProfileResponse,
  UserRegisterRequest,
  UserLoginRequest,
} from '../../types/api';

export interface AuthContextValue {
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

export const AuthContext = createContext<AuthContextValue | null>(null);

const getStorageKey = (pizzeriaCode: string) =>
  `pizzeria-${pizzeriaCode}-auth-token`;

interface AuthProviderProps {
  children: ReactNode;
  pizzeriaCode?: string;
}

export const AuthProvider = ({ children, pizzeriaCode }: AuthProviderProps) => {
  const [profile, setProfile] = useState<UserProfileResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const clearAuth = useCallback(() => {
    setAuthToken(null);
    setProfile(null);
    if (pizzeriaCode) {
      localStorage.removeItem(getStorageKey(pizzeriaCode));
    }
  }, [pizzeriaCode]);

  const refreshProfile = useCallback(async () => {
    try {
      const response = await authApi.fetchProfile();
      setProfile(response.data);
    } catch {
      clearAuth();
    }
  }, [clearAuth]);

  // Initialize auth state on mount
  useEffect(() => {
    const initAuth = async () => {
      if (!pizzeriaCode) {
        setIsLoading(false);
        return;
      }

      const storedToken = localStorage.getItem(getStorageKey(pizzeriaCode));
      if (storedToken) {
        setAuthToken(storedToken);
        try {
          await refreshProfile();
        } catch {
          clearAuth();
        }
      }
      setIsLoading(false);
    };

    void initAuth();
  }, [pizzeriaCode, refreshProfile, clearAuth]);

  // Listen for unauthorized events
  useEffect(() => {
    const handleUnauthorized = () => {
      clearAuth();
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => {
      window.removeEventListener('auth:unauthorized', handleUnauthorized);
    };
  }, [clearAuth]);

  const login = useCallback(
    async (code: string, payload: UserLoginRequest) => {
      const response = await authApi.login(code, payload);
      const { accessToken } = response.data;

      setAuthToken(accessToken);
      localStorage.setItem(getStorageKey(code), accessToken);

      await refreshProfile();
    },
    [refreshProfile]
  );

  const register = useCallback(
    async (code: string, payload: UserRegisterRequest) => {
      const response = await authApi.register(code, payload);
      return { verificationToken: response.data.verificationToken };
    },
    []
  );

  const logout = useCallback(async () => {
    try {
      if (getAuthToken()) {
        await authApi.logout();
      }
    } finally {
      clearAuth();
    }
  }, [clearAuth]);

  const deleteAccount = useCallback(async () => {
    await authApi.deleteProfile();
    clearAuth();
  }, [clearAuth]);

  return (
    <AuthContext.Provider
      value={{
        isAuthenticated: !!profile,
        isLoading,
        profile,
        login,
        register,
        logout,
        refreshProfile,
        deleteAccount,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export default AuthProvider;

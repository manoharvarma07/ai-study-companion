import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api, getToken, setOnUnauthorized, setToken } from '../api/client';
import { authApi } from '../api/services';
import type { User } from '../api/types';

interface AuthState {
  user: User | null;
  loading: boolean;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  const logout = useCallback(() => {
    setToken(null);
    setUser(null);
    delete api.defaults.headers.common.Authorization;
  }, []);

  useEffect(() => {
    setOnUnauthorized(() => logout());
    const token = getToken();
    if (!token) {
      setLoading(false);
      return;
    }
    authApi
      .me()
      .then(setUser)
      .catch(() => logout())
      .finally(() => setLoading(false));
    return () => setOnUnauthorized(null);
  }, [logout]);

  const login = useCallback(async (email: string, password: string) => {
    const res = await authApi.login(email, password);
    setToken(res.token);
    setUser(res.user);
  }, []);

  const register = useCallback(async (name: string, email: string, password: string) => {
    const res = await authApi.register(name, email, password);
    // Backend returns AuthResponse on register too; log in directly if token present.
    if (res?.token) {
      setToken(res.token);
      setUser(res.user);
    }
  }, []);

  const value = useMemo(
    () => ({ user, loading, isAuthenticated: !!user, login, register, logout }),
    [user, loading, login, register, logout],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}

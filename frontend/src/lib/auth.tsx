import { useQueryClient } from '@tanstack/react-query';
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { clearToken, getToken, setToken, setUnauthorizedHandler } from '../api/client';

const EMAIL_KEY = 'siseradar-email';

interface AuthContextValue {
  email: string | null;
  isAuthenticated: boolean;
  login: (token: string, email: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [email, setEmail] = useState<string | null>(
    () => (getToken() ? localStorage.getItem(EMAIL_KEY) : null),
  );

  const logout = useCallback(() => {
    clearToken();
    localStorage.removeItem(EMAIL_KEY);
    setEmail(null);
    // drop any user-scoped cache (watchlist, alerts, notifications)
    queryClient.removeQueries({ queryKey: ['me'] });
  }, [queryClient]);

  const login = useCallback(
    (token: string, userEmail: string) => {
      setToken(token);
      localStorage.setItem(EMAIL_KEY, userEmail);
      setEmail(userEmail);
    },
    [],
  );

  // When the API rejects our token, log out and send the user to login.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      logout();
      navigate('/login');
    });
    return () => setUnauthorizedHandler(null);
  }, [logout, navigate]);

  const value = useMemo(
    () => ({ email, isAuthenticated: email !== null, login, logout }),
    [email, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}

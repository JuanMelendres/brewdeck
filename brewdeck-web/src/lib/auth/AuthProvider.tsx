'use client';

import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import {
  getMe,
  login as loginApi,
  register as registerApi,
  updateProfile as updateProfileApi,
} from '@/lib/api/auth';
import type { UserResponse } from '@/lib/api/types';
import { clearToken, getToken, setToken } from './tokenStore';

type AuthStatus = 'loading' | 'authenticated' | 'anonymous';

type Credentials = { email: string; password: string };

type AuthContextValue = {
  user: UserResponse | null;
  status: AuthStatus;
  login: (body: Credentials) => Promise<void>;
  register: (body: Credentials) => Promise<void>;
  updateProfile: (body: { displayName: string | null }) => Promise<void>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  // Initialise status without a token check in the effect body to avoid
  // synchronous setState inside useEffect (react-hooks/set-state-in-effect).
  const [status, setStatus] = useState<AuthStatus>(() =>
    getToken() ? 'loading' : 'anonymous',
  );

  useEffect(() => {
    if (!getToken()) {
      return;
    }
    getMe()
      .then((me) => {
        setUser(me);
        setStatus('authenticated');
      })
      .catch(() => {
        clearToken();
        setUser(null);
        setStatus('anonymous');
      });
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      status,
      login: async (body) => {
        const response = await loginApi(body);
        setToken(response.token);
        const me = await getMe();
        setUser(me);
        setStatus('authenticated');
      },
      register: async (body) => {
        const response = await registerApi(body);
        setToken(response.token);
        const me = await getMe();
        setUser(me);
        setStatus('authenticated');
      },
      updateProfile: async (body) => {
        const updated = await updateProfileApi(body);
        setUser(updated);
      },
      logout: () => {
        clearToken();
        setUser(null);
        setStatus('anonymous');
      },
    }),
    [user, status],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

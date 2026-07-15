'use client';

import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import {
  getMe,
  login as loginApi,
  logout as logoutApi,
  register as registerApi,
  updateProfile as updateProfileApi,
} from '@/lib/api/auth';
import type { UserResponse } from '@/lib/api/types';
import { clearTokens, getRefreshToken, getToken, setRefreshToken, setToken } from './tokenStore';

type AuthStatus = 'loading' | 'authenticated' | 'anonymous';

type Credentials = { email: string; password: string };

type AuthContextValue = {
  user: UserResponse | null;
  status: AuthStatus;
  login: (body: Credentials) => Promise<void>;
  register: (body: Credentials) => Promise<void>;
  updateProfile: (body: { displayName: string | null }) => Promise<void>;
  refreshUser: () => Promise<void>;
  logout: () => Promise<void>;
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
        clearTokens();
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
        setRefreshToken(response.refreshToken);
        const me = await getMe();
        setUser(me);
        setStatus('authenticated');
      },
      register: async (body) => {
        const response = await registerApi(body);
        setToken(response.token);
        setRefreshToken(response.refreshToken);
        const me = await getMe();
        setUser(me);
        setStatus('authenticated');
      },
      updateProfile: async (body) => {
        const updated = await updateProfileApi(body);
        setUser(updated);
      },
      refreshUser: async () => {
        if (!getToken()) {
          return;
        }
        try {
          const me = await getMe();
          setUser(me);
        } catch {
          // Ignore: a failed refresh leaves the existing user state untouched.
        }
      },
      logout: async () => {
        const refreshToken = getRefreshToken();
        setUser(null);
        setStatus('anonymous');
        try {
          if (refreshToken) {
            await logoutApi(refreshToken);
          }
        } catch {
          // Best-effort server revoke; local sign-out already done.
        } finally {
          clearTokens();
        }
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

'use client';

import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import {
  getMe,
  login as loginApi,
  logout as logoutApi,
  register as registerApi,
  updateProfile as updateProfileApi,
  updateLanguage as updateLanguageApi,
  updateTheme as updateThemeApi,
} from '@/lib/api/auth';
import type { Language, ThemePreference, UserResponse } from '@/lib/api/types';
import { refreshSession } from '@/lib/api/client';
import { clearTokens, getToken, purgeLegacyTokenStorage, setToken } from './tokenStore';

type AuthStatus = 'loading' | 'authenticated' | 'anonymous';

type Credentials = { email: string; password: string };

type AuthContextValue = {
  user: UserResponse | null;
  status: AuthStatus;
  login: (body: Credentials) => Promise<void>;
  register: (body: Credentials) => Promise<void>;
  updateProfile: (body: { displayName: string | null }) => Promise<void>;
  updateTheme: (themePreference: ThemePreference) => Promise<void>;
  updateLanguage: (language: Language) => Promise<void>;
  refreshUser: () => Promise<void>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  // Always start loading: whether a session exists is only known after asking the server,
  // because the refresh token is an httpOnly cookie scripts can't see (ADR-013).
  const [status, setStatus] = useState<AuthStatus>('loading');

  useEffect(() => {
    purgeLegacyTokenStorage();
    let cancelled = false;
    refreshSession()
      .then(() => getMe())
      .then((me) => {
        if (!cancelled) {
          setUser(me);
          setStatus('authenticated');
        }
      })
      .catch(() => {
        if (!cancelled) {
          clearTokens();
          setUser(null);
          setStatus('anonymous');
        }
      });
    return () => {
      cancelled = true;
    };
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
      updateTheme: async (themePreference) => {
        const updated = await updateThemeApi({ themePreference });
        setUser(updated);
      },
      updateLanguage: async (language) => {
        const updated = await updateLanguageApi({ language });
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
        setUser(null);
        setStatus('anonymous');
        try {
          // Revokes the cookie's refresh token server-side and clears the cookie.
          await logoutApi();
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

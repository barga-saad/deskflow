import { useQueryClient } from '@tanstack/react-query';
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { api, tokenStore } from './api';
import type { AuthResponse, User } from './api';

interface AuthContextValue {
  user: User | null;
  loading: boolean;
  isStaff: boolean;
  signIn: (auth: AuthResponse) => void;
  signOut: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(Boolean(tokenStore.get()));

  useEffect(() => {
    if (!tokenStore.get()) {
      return;
    }
    api.me()
      .then(setUser)
      .catch(() => tokenStore.clear())
      .finally(() => setLoading(false));
  }, []);

  const signIn = useCallback((auth: AuthResponse) => {
    tokenStore.set(auth.token);
    setUser(auth.user);
  }, []);

  const signOut = useCallback(() => {
    tokenStore.clear();
    setUser(null);
    // Cached data belongs to this user, so it must not survive a logout.
    queryClient.clear();
  }, [queryClient]);

  const value = useMemo(
    () => ({ user, loading, isStaff: user?.role === 'AGENT' || user?.role === 'ADMIN', signIn, signOut }),
    [user, loading, signIn, signOut],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return context;
}

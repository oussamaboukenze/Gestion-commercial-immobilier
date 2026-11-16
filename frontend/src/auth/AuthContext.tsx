import { createContext, useContext, type ReactNode } from 'react';
import { keycloak, utilisateurCourant, type Role, type Utilisateur } from './keycloak';

interface Auth extends Utilisateur {
  hasRole: (...roles: Role[]) => boolean;
  logout: () => void;
}

const AuthContext = createContext<Auth | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const u = utilisateurCourant();
  const value: Auth = {
    ...u,
    hasRole: (...roles) => roles.some((r) => u.roles.includes(r)),
    logout: () => keycloak.logout({ redirectUri: window.location.origin }),
  };
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): Auth {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth hors AuthProvider');
  return ctx;
}

import Keycloak from 'keycloak-js';

/** Connexion Keycloak (realm "ahi", client public "gecimmo-web", PKCE). */
export const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM ?? 'ahi',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT ?? 'gecimmo-web',
});

export type Role =
  | 'COMMERCIAL' | 'ADV' | 'COMPTABLE' | 'ADMIN_SI' | 'RESP_COMMERCIAL' | 'DAF' | 'SAV' | 'DIRECTION';

export interface Utilisateur {
  username: string;
  nom: string;
  roles: Role[];
  projets: string[];
}

export function utilisateurCourant(): Utilisateur {
  const t = (keycloak.tokenParsed ?? {}) as Record<string, unknown>;
  const realmAccess = (t.realm_access as { roles?: string[] } | undefined)?.roles ?? [];
  const groups = (t.groups as string[] | undefined) ?? [];
  return {
    username: String(t.preferred_username ?? ''),
    nom: String(t.name ?? t.preferred_username ?? ''),
    roles: realmAccess.filter((r): r is Role =>
      ['COMMERCIAL', 'ADV', 'COMPTABLE', 'ADMIN_SI', 'RESP_COMMERCIAL', 'DAF', 'SAV', 'DIRECTION'].includes(r)),
    projets: groups.map((g) => g.replace(/^\//, '')).filter((g) => g.startsWith('projet-'))
      .map((g) => g.substring('projet-'.length)),
  };
}

/** Token toujours frais (rafraîchi s'il expire dans moins de 30 s). */
export async function tokenValide(): Promise<string> {
  try {
    await keycloak.updateToken(30);
  } catch {
    await keycloak.login();
  }
  return keycloak.token ?? '';
}

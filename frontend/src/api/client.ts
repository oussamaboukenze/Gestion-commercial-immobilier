import { tokenValide } from '../auth/keycloak';

export const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

/** Erreur métier renvoyée par le backend (ProblemDetail : { detail, erreurs }). */
export class ApiError extends Error {
  constructor(public status: number, message: string, public erreurs?: Record<string, string>) {
    super(message);
  }
}

async function requete<T>(methode: string, chemin: string, corps?: unknown): Promise<T> {
  const token = await tokenValide();
  const estFormData = corps instanceof FormData;
  const resp = await fetch(API_URL + chemin, {
    method: methode,
    headers: {
      Authorization: `Bearer ${token}`,
      ...(corps !== undefined && !estFormData ? { 'Content-Type': 'application/json' } : {}),
    },
    body: corps === undefined ? undefined : estFormData ? corps : JSON.stringify(corps),
  });
  if (!resp.ok) {
    let message = `Erreur ${resp.status}`;
    let erreurs: Record<string, string> | undefined;
    try {
      const pb = await resp.json();
      message = pb.detail ?? pb.title ?? message;
      erreurs = pb.erreurs;
      if (erreurs) message += ' : ' + Object.entries(erreurs).map(([k, v]) => `${k} ${v}`).join(', ');
    } catch {
      if (resp.status === 401) message = 'Session expirée, reconnectez-vous.';
      if (resp.status === 403) message = 'Action hors de votre périmètre.';
      if (resp.status === 503) message = 'Service indisponible, réessayez dans un instant.';
    }
    throw new ApiError(resp.status, message, erreurs);
  }
  if (resp.status === 204) return undefined as T;
  const type = resp.headers.get('Content-Type') ?? '';
  return (type.includes('application/json') ? resp.json() : resp.text()) as Promise<T>;
}

export const api = {
  get: <T,>(chemin: string) => requete<T>('GET', chemin),
  post: <T,>(chemin: string, corps?: unknown) => requete<T>('POST', chemin, corps ?? {}),
  put: <T,>(chemin: string, corps?: unknown) => requete<T>('PUT', chemin, corps ?? {}),
  upload: <T,>(chemin: string, form: FormData) => requete<T>('POST', chemin, form),
};

/** Ouvre un document (HTML imprimable, PDF, image, CSV) dans un nouvel onglet ou le télécharge. */
export async function ouvrirDocument(chemin: string, nomTelechargement?: string) {
  const token = await tokenValide();
  const resp = await fetch(API_URL + chemin, { headers: { Authorization: `Bearer ${token}` } });
  if (!resp.ok) {
    let message = `Erreur ${resp.status}`;
    try { message = (await resp.json()).detail ?? message; } catch { /* corps non JSON */ }
    throw new ApiError(resp.status, message);
  }
  const blob = await resp.blob();
  const url = URL.createObjectURL(blob);
  if (nomTelechargement) {
    const a = document.createElement('a');
    a.href = url;
    a.download = nomTelechargement;
    a.click();
  } else {
    window.open(url, '_blank');
  }
  setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

/** Construit une query string en ignorant les valeurs vides. */
export function qs(params: Record<string, string | number | undefined | null>): string {
  const p = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') p.set(k, String(v));
  });
  const s = p.toString();
  return s ? `?${s}` : '';
}

import { Alert, Snackbar } from '@mui/material';
import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';

type Severite = 'success' | 'error' | 'info' | 'warning';
const NotifierContext = createContext<(message: string, severite?: Severite) => void>(() => {});

/** Messages courts en bas de l'écran (succès / erreur métier). */
export function NotifierProvider({ children }: { children: ReactNode }) {
  const [msg, setMsg] = useState<{ texte: string; severite: Severite; cle: number } | null>(null);
  const notifier = useCallback((texte: string, severite: Severite = 'success') => {
    setMsg({ texte, severite, cle: Date.now() });
  }, []);
  return (
    <NotifierContext.Provider value={notifier}>
      {children}
      <Snackbar key={msg?.cle} open={!!msg} autoHideDuration={msg?.severite === 'error' ? 8000 : 4000}
        onClose={() => setMsg(null)} anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
        {msg ? <Alert severity={msg.severite} variant="filled" onClose={() => setMsg(null)}>{msg.texte}</Alert> : undefined}
      </Snackbar>
    </NotifierContext.Provider>
  );
}

export const useNotifier = () => useContext(NotifierContext);

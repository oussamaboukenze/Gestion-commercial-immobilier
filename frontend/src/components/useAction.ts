import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNotifier } from './Notifier';

/**
 * Exécute une action (POST/PUT), affiche le résultat et rafraîchit les listes.
 * Toutes les requêtes sont invalidées : simple et toujours à jour pour un back-office.
 */
export function useAction<V = void, R = unknown>(fn: (v: V) => Promise<R>, messageSucces?: string) {
  const notifier = useNotifier();
  const qc = useQueryClient();
  return useMutation<R, Error, V>({
    mutationFn: fn,
    onSuccess: () => {
      if (messageSucces) notifier(messageSucces);
      qc.invalidateQueries();
    },
    onError: (e) => notifier(e.message, 'error'),
  });
}

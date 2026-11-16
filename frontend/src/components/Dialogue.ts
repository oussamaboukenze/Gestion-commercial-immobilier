import { useState } from 'react';
import type { Valeurs } from './FormDialog';

/** Petit état pour ouvrir une boîte de dialogue avec une "cible" (ligne sélectionnée) et des valeurs initiales. */
export function useDialogue<T = undefined>() {
  const [etat, setEtat] = useState<{ ouvert: boolean; cible?: T; initial?: Valeurs }>({ ouvert: false });
  return {
    ouvert: etat.ouvert,
    cible: etat.cible,
    initial: etat.initial,
    ouvrir: (cible?: T, initial?: Valeurs) => setEtat({ ouvert: true, cible, initial }),
    fermer: () => setEtat({ ouvert: false }),
  };
}

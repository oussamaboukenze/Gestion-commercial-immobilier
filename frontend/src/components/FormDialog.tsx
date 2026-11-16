import {
  Button, Checkbox, Dialog, DialogActions, DialogContent, DialogTitle, FormControlLabel, MenuItem, Stack, TextField,
  Typography,
} from '@mui/material';
import { useEffect, useState, type ReactNode } from 'react';
import { libelle } from '../format';

export interface Champ {
  nom: string;
  label: string;
  type?: 'text' | 'number' | 'date' | 'datetime-local' | 'select' | 'textarea' | 'checkbox' | 'multiselect' | 'email';
  options?: (string | { valeur: string | number; label: string })[];
  requis?: boolean;
  aide?: string;
}

export type Valeurs = Record<string, unknown>;

interface Props {
  ouvert: boolean;
  titre: string;
  champs?: Champ[];
  initial?: Valeurs;
  texte?: ReactNode;
  libelleValider?: string;
  danger?: boolean;
  enCours?: boolean;
  onFermer: () => void;
  onValider: (v: Valeurs) => void;
}

/** Formulaire générique en boîte de dialogue (aussi utilisé comme simple confirmation sans champs). */
export default function FormDialog({
  ouvert, titre, champs = [], initial, texte, libelleValider = 'Enregistrer', danger, enCours, onFermer, onValider,
}: Props) {
  const [v, setV] = useState<Valeurs>({});
  useEffect(() => {
    if (ouvert) setV({ ...(initial ?? {}) });
  }, [ouvert]); // eslint-disable-line react-hooks/exhaustive-deps

  const manquant = champs.some((c) => c.requis && (v[c.nom] === undefined || v[c.nom] === '' || v[c.nom] === null));
  const maj = (nom: string, valeur: unknown) => setV((p) => ({ ...p, [nom]: valeur }));

  return (
    <Dialog open={ouvert} onClose={onFermer} fullWidth maxWidth="sm">
      <DialogTitle>{titre}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          {texte && <Typography variant="body2" component="div">{texte}</Typography>}
          {champs.map((c) => {
            if (c.type === 'checkbox') {
              return (
                <FormControlLabel key={c.nom} label={c.label}
                  control={<Checkbox checked={!!v[c.nom]} onChange={(e) => maj(c.nom, e.target.checked)} />} />
              );
            }
            const options = (c.options ?? []).map((o) => (typeof o === 'string' ? { valeur: o, label: libelle(o) } : o));
            const estSelect = c.type === 'select' || c.type === 'multiselect';
            return (
              <TextField key={c.nom} label={c.label} required={c.requis} helperText={c.aide} size="small"
                type={estSelect || c.type === 'textarea' ? undefined : c.type ?? 'text'}
                select={estSelect} multiline={c.type === 'textarea'} minRows={c.type === 'textarea' ? 3 : undefined}
                slotProps={{
                  inputLabel: c.type === 'date' || c.type === 'datetime-local' ? { shrink: true } : undefined,
                  select: c.type === 'multiselect' ? { multiple: true } : undefined,
                }}
                value={v[c.nom] ?? (c.type === 'multiselect' ? [] : '')}
                onChange={(e) => maj(c.nom, c.type === 'number' && e.target.value !== ''
                  ? Number(e.target.value) : e.target.value)}>
                {options.map((o) => <MenuItem key={o.valeur} value={o.valeur}>{o.label}</MenuItem>)}
              </TextField>
            );
          })}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onFermer}>Annuler</Button>
        <Button variant="contained" color={danger ? 'error' : 'primary'} disabled={manquant || enCours}
          onClick={() => onValider(v)}>
          {enCours ? 'En cours…' : libelleValider}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

import { Button, MenuItem, Stack, TextField } from '@mui/material';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import { useRef, useState } from 'react';
import { api } from '../api/client';
import { libelle } from '../format';
import { useAction } from './useAction';

const TYPES = ['CIN', 'JUSTIFICATIF_REVENUS', 'RECU_AVANCE', 'AUTRE'];

/** Dépôt d'une pièce du dossier (PDF / JPEG / PNG, 10 Mo max). */
export default function Televersement({ reservationId }: { reservationId: number }) {
  const [type, setType] = useState('CIN');
  const input = useRef<HTMLInputElement>(null);
  const deposer = useAction((f: File) => {
    const form = new FormData();
    form.append('fichier', f);
    return api.upload(`/api/ventes/reservations/${reservationId}/pieces?type=${type}`, form);
  }, 'Pièce déposée');
  return (
    <Stack direction="row" spacing={1} alignItems="center">
      <TextField select size="small" label="Type de pièce" value={type} onChange={(e) => setType(e.target.value)} sx={{ minWidth: 220 }}>
        {TYPES.map((t) => <MenuItem key={t} value={t}>{libelle(t)}</MenuItem>)}
      </TextField>
      <input ref={input} type="file" hidden accept="application/pdf,image/jpeg,image/png"
        onChange={(e) => { const f = e.target.files?.[0]; if (f) deposer.mutate(f); e.target.value = ''; }} />
      <Button variant="outlined" startIcon={<UploadFileIcon />} disabled={deposer.isPending} onClick={() => input.current?.click()}>
        {deposer.isPending ? 'Envoi…' : 'Déposer un fichier'}
      </Button>
    </Stack>
  );
}

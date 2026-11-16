import { Box, Button, Checkbox, Grid, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { api } from '../../api/client';
import type { Encaissement, Impaye, Remise } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import DataTable from '../../components/DataTable';
import { useDialogue } from '../../components/Dialogue';
import FormDialog from '../../components/FormDialog';
import PageHeader from '../../components/PageHeader';
import { useAction } from '../../components/useAction';
import { aujourdhui, date, dh } from '../../format';

/** Trésorerie > Remises en banque + Impayés. */
export default function Banque() {
  const { hasRole } = useAuth();
  const comptable = hasRole('COMPTABLE');
  const [selection, setSelection] = useState<number[]>([]);
  const cheques = useQuery({ queryKey: ['cheques-a-remettre'], queryFn: () => api.get<Encaissement[]>('/api/tresorerie/remises/cheques-a-remettre') });
  const remises = useQuery({ queryKey: ['remises'], queryFn: () => api.get<Remise[]>('/api/tresorerie/remises') });
  const impayes = useQuery({ queryKey: ['impayes'], queryFn: () => api.get<Impaye[]>('/api/tresorerie/impayes') });
  const dRemise = useDialogue();
  const remettre = useAction((v: Record<string, unknown>) => api.post('/api/tresorerie/remises', { ...v, encaissementIds: selection }), 'Bordereau de remise créé');
  const total = (cheques.data ?? []).filter((c) => selection.includes(c.id)).reduce((s, c) => s + c.montant, 0);
  const basculer = (id: number) => setSelection((s) => (s.includes(id) ? s.filter((x) => x !== id) : [...s, id]));

  return (
    <Box>
      <PageHeader titre="Remises en banque & impayés"
        actions={comptable && <Button variant="contained" disabled={selection.length === 0} onClick={() => dRemise.ouvrir(undefined, { dateRemise: aujourdhui() })}>
          Remettre {selection.length} chèque(s) · {dh(total)}</Button>} />
      <Typography variant="subtitle1" fontWeight={600} gutterBottom>Chèques à remettre</Typography>
      <DataTable cle={(c) => c.id} lignes={cheques.data} chargement={cheques.isLoading} vide="Aucun chèque en attente de remise"
        colonnes={[
          { titre: '', rendu: (c) => comptable && <Checkbox size="small" checked={selection.includes(c.id)} onChange={() => basculer(c.id)} />, largeur: 40 },
          { titre: 'Reçu', rendu: (c) => c.numeroRecu },
          { titre: 'Date', rendu: (c) => date(c.dateEncaissement) },
          { titre: 'Client', rendu: (c) => c.clientNom },
          { titre: 'Chèque', rendu: (c) => `n° ${c.numeroCheque} · ${c.banque}` },
          { titre: 'Montant', rendu: (c) => dh(c.montant), align: 'right' },
        ]} />
      <Grid container spacing={3} sx={{ mt: 1 }}>
        <Grid size={{ xs: 12, md: 6 }}>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>Bordereaux de remise</Typography>
          <DataTable cle={(r) => r.id} lignes={remises.data} chargement={remises.isLoading} vide="Aucune remise"
            colonnes={[
              { titre: 'N°', rendu: (r) => r.numero },
              { titre: 'Date', rendu: (r) => date(r.dateRemise) },
              { titre: 'Banque', rendu: (r) => r.banque },
              { titre: 'Chèques', rendu: (r) => r.nombreCheques, align: 'center' },
              { titre: 'Total', rendu: (r) => dh(r.montantTotal), align: 'right' },
            ]} />
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>Chèques impayés</Typography>
          <DataTable cle={(i) => i.id} lignes={impayes.data} chargement={impayes.isLoading} vide="Aucun impayé"
            colonnes={[
              { titre: 'Rejet', rendu: (i) => date(i.dateRejet) },
              { titre: 'Client', rendu: (i) => `${i.clientNom} (${i.reservationNumero})` },
              { titre: 'Chèque', rendu: (i) => i.numeroCheque },
              { titre: 'Montant', rendu: (i) => dh(i.montant), align: 'right' },
              { titre: 'Motif', rendu: (i) => i.motif },
            ]} />
        </Grid>
      </Grid>
      <FormDialog ouvert={dRemise.ouvert} titre="Remise en banque" initial={dRemise.initial} onFermer={dRemise.fermer}
        texte={`${selection.length} chèque(s) pour un total de ${dh(total)}`} libelleValider="Créer le bordereau"
        champs={[{ nom: 'banque', label: 'Banque de remise', requis: true }, { nom: 'dateRemise', label: 'Date', type: 'date', requis: true }]}
        onValider={(v) => remettre.mutate(v, { onSuccess: () => { dRemise.fermer(); setSelection([]); } })} />
    </Box>
  );
}

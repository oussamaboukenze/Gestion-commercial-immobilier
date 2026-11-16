import { Box, Button, Link, MenuItem, Pagination, Stack, TextField } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { api, ouvrirDocument, qs } from '../../api/client';
import type { Encaissement, Page } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import DataTable from '../../components/DataTable';
import { useDialogue } from '../../components/Dialogue';
import FormDialog, { type Champ } from '../../components/FormDialog';
import { useNotifier } from '../../components/Notifier';
import PageHeader from '../../components/PageHeader';
import StatutChip from '../../components/StatutChip';
import { useAction } from '../../components/useAction';
import { aujourdhui, date, dh, libelle } from '../../format';

export const CHAMPS_ENCAISSEMENT: Champ[] = [
  { nom: 'reservationId', label: 'Id du dossier (réservation)', type: 'number', requis: true },
  { nom: 'montant', label: 'Montant (DH)', type: 'number', requis: true },
  { nom: 'mode', label: 'Mode', type: 'select', options: ['ESPECES', 'CHEQUE', 'VIREMENT', 'VERSEMENT'], requis: true },
  { nom: 'numeroCheque', label: 'N° de chèque', aide: 'Obligatoire pour un chèque' },
  { nom: 'banque', label: 'Banque', aide: 'Obligatoire pour un chèque' },
  { nom: 'reference', label: 'Référence (virement / versement)' },
  { nom: 'dateEncaissement', label: "Date d'encaissement", type: 'date', requis: true },
];

/** Trésorerie > Encaissements. */
export default function Encaissements() {
  const { hasRole } = useAuth();
  const notifier = useNotifier();
  const comptable = hasRole('COMPTABLE');
  const [mode, setMode] = useState('');
  const [du, setDu] = useState('');
  const [au, setAu] = useState('');
  const [page, setPage] = useState(0);
  const liste = useQuery({
    queryKey: ['encaissements', mode, du, au, page],
    queryFn: () => api.get<Page<Encaissement>>('/api/tresorerie/encaissements' + qs({ mode, du, au, page, size: 20 })),
  });
  const dNouveau = useDialogue();
  const dImpaye = useDialogue<Encaissement>();
  const dCorrection = useDialogue<Encaissement>();
  const enregistrer = useAction((v: Record<string, unknown>) => api.post('/api/tresorerie/encaissements', v), 'Encaissement enregistré, reçu généré');
  const impaye = useAction((v: Record<string, unknown>) => api.post('/api/tresorerie/impayes', v), 'Impayé enregistré : échéance due + relance créée');
  const correction = useAction((v: { id: number; motif: unknown }) => api.post(`/api/tresorerie/encaissements/${v.id}/contre-passation`, { motif: v.motif }),
    'Demande de contre-passation envoyée au DAF');

  return (
    <Box>
      <PageHeader titre="Encaissements" sousTitre="Un encaissement validé ne se supprime jamais : contre-passation validée par le DAF"
        actions={comptable && <Button variant="contained" startIcon={<AddIcon />} onClick={() => dNouveau.ouvrir(undefined, { mode: 'CHEQUE', dateEncaissement: aujourdhui() })}>Nouveau</Button>} />
      <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
        <TextField select size="small" label="Mode" value={mode} onChange={(e) => { setMode(e.target.value); setPage(0); }} sx={{ minWidth: 150 }}>
          <MenuItem value="">Tous</MenuItem>
          {['ESPECES', 'CHEQUE', 'VIREMENT', 'VERSEMENT'].map((m) => <MenuItem key={m} value={m}>{libelle(m)}</MenuItem>)}
        </TextField>
        <TextField size="small" type="date" label="Du" value={du} onChange={(e) => setDu(e.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
        <TextField size="small" type="date" label="Au" value={au} onChange={(e) => setAu(e.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
      </Stack>
      <DataTable cle={(e) => e.id} lignes={liste.data?.content} chargement={liste.isLoading}
        colonnes={[
          { titre: 'Reçu', rendu: (e) => <Link component="button" onClick={() => ouvrirDocument(`/api/tresorerie/encaissements/${e.id}/recu`).catch((x: Error) => notifier(x.message, 'error'))}>{e.numeroRecu}</Link> },
          { titre: 'Date', rendu: (e) => date(e.dateEncaissement) },
          { titre: 'Dossier', rendu: (e) => e.reservationNumero },
          { titre: 'Client', rendu: (e) => e.clientNom },
          { titre: 'Mode', rendu: (e) => `${libelle(e.mode)}${e.numeroCheque ? ` n° ${e.numeroCheque}` : ''}` },
          { titre: 'Montant', rendu: (e) => dh(e.montant), align: 'right' },
          { titre: 'Remise', rendu: (e) => e.remiseNumero ?? '—' },
          { titre: 'Statut', rendu: (e) => <StatutChip statut={e.statut} /> },
          {
            titre: '', rendu: (e) => comptable && e.statut === 'VALIDE' && (
              <Stack direction="row" spacing={1}>
                {e.mode === 'CHEQUE' && <Button size="small" color="warning" onClick={() => dImpaye.ouvrir(e, { dateRejet: aujourdhui() })}>Impayé</Button>}
                <Button size="small" onClick={() => dCorrection.ouvrir(e)}>Contre-passer</Button>
              </Stack>
            ),
          },
        ]} />
      {(liste.data?.page.totalPages ?? 0) > 1 &&
        <Pagination sx={{ mt: 2 }} count={liste.data!.page.totalPages} page={page + 1} onChange={(_, p) => setPage(p - 1)} />}
      <FormDialog ouvert={dNouveau.ouvert} titre="Nouvel encaissement" initial={dNouveau.initial} champs={CHAMPS_ENCAISSEMENT}
        onFermer={dNouveau.fermer} enCours={enregistrer.isPending} libelleValider="Encaisser"
        onValider={(v) => enregistrer.mutate(v, { onSuccess: dNouveau.fermer })} />
      <FormDialog ouvert={dImpaye.ouvert} titre={`Chèque impayé — reçu ${dImpaye.cible?.numeroRecu ?? ''}`} initial={dImpaye.initial} onFermer={dImpaye.fermer}
        texte="L'échéance redevient due et une relance est créée automatiquement. Pensez à informer l'ADV." danger libelleValider="Déclarer impayé"
        champs={[{ nom: 'motif', label: 'Motif du rejet', requis: true }, { nom: 'dateRejet', label: 'Date du rejet', type: 'date', requis: true }]}
        onValider={(v) => impaye.mutate({ ...v, encaissementId: dImpaye.cible!.id }, { onSuccess: dImpaye.fermer })} />
      <FormDialog ouvert={dCorrection.ouvert} titre={`Contre-passation — reçu ${dCorrection.cible?.numeroRecu ?? ''}`} onFermer={dCorrection.fermer}
        texte="La correction ne sera appliquée qu'après validation du DAF." libelleValider="Envoyer au DAF"
        champs={[{ nom: 'motif', label: 'Motif', type: 'textarea', requis: true }]}
        onValider={(v) => correction.mutate({ id: dCorrection.cible!.id, motif: v.motif }, { onSuccess: dCorrection.fermer })} />
    </Box>
  );
}

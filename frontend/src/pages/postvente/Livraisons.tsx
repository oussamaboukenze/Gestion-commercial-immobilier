import { Box, Button, Stack, TextField, Typography } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { api, ouvrirDocument, qs } from '../../api/client';
import type { DossierNotaire, Livraison } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import DataTable from '../../components/DataTable';
import { useDialogue } from '../../components/Dialogue';
import FormDialog from '../../components/FormDialog';
import { useNotifier } from '../../components/Notifier';
import PageHeader from '../../components/PageHeader';
import StatutChip from '../../components/StatutChip';
import { useAction } from '../../components/useAction';
import { aujourdhui, date } from '../../format';

const dans = (jours: number) => new Date(Date.now() + jours * 86_400_000).toISOString().slice(0, 10);

/** Livraison > Planning / Pré-réception / PV de livraison / Remise des clés. */
export default function Livraisons() {
  const { hasRole } = useAuth();
  const notifier = useNotifier();
  const adv = hasRole('ADV');
  const [du, setDu] = useState(dans(-30));
  const [au, setAu] = useState(dans(90));
  const planning = useQuery({ queryKey: ['planning', du, au], queryFn: () => api.get<Livraison[]>('/api/postvente/livraisons/planning' + qs({ du, au })) });
  const livrees = useQuery({ queryKey: ['livrees'], queryFn: () => api.get<Livraison[]>('/api/postvente/livraisons/livrees') });
  const actes = useQuery({ queryKey: ['notaire-actes'], enabled: adv, queryFn: () => api.get<DossierNotaire[]>('/api/postvente/notaire/actes') });
  const dPlan = useDialogue();
  const dDate = useDialogue<Livraison>();
  const dPre = useDialogue<Livraison>();
  const dPv = useDialogue<Livraison>();
  const planifier = useAction((v: Record<string, unknown>) => api.post('/api/postvente/livraisons', v), 'Livraison planifiée');
  const replanifier = useAction((v: { id: number; d: unknown }) => api.put(`/api/postvente/livraisons/${v.id}/date`, { datePrevue: v.d }), 'Date modifiée');
  const pre = useAction((v: { id: number; corps: Record<string, unknown> }) => api.post(`/api/postvente/livraisons/${v.id}/pre-reception`, v.corps), 'Pré-réception enregistrée');
  const lever = useAction((id: number) => api.post(`/api/postvente/livraisons/${id}/lever-reserves`), 'Réserves levées');
  const pv = useAction((v: { id: number; corps: Record<string, unknown> }) => api.post(`/api/postvente/livraisons/${v.id}/pv`, v.corps), 'Lot livré, clés remises');

  return (
    <Box>
      <PageHeader titre="Livraisons" sousTitre="PV de livraison et remise des clés uniquement si le solde est à 0"
        actions={adv && <Button variant="contained" startIcon={<AddIcon />} onClick={() => dPlan.ouvrir(undefined, { datePrevue: dans(14) })}>Planifier</Button>} />
      <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
        <TextField size="small" type="date" label="Du" value={du} onChange={(e) => setDu(e.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
        <TextField size="small" type="date" label="Au" value={au} onChange={(e) => setAu(e.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
      </Stack>
      <DataTable cle={(l) => l.id} lignes={planning.data} chargement={planning.isLoading} vide="Aucune livraison planifiée sur la période"
        colonnes={[
          { titre: 'Date prévue', rendu: (l) => date(l.datePrevue) },
          { titre: 'Dossier', rendu: (l) => l.reservationNumero },
          { titre: 'Client', rendu: (l) => <>{l.clientNom}<br /><small>{l.clientTelephone}</small></> },
          { titre: 'Lot', rendu: (l) => l.lotCode },
          { titre: 'Réserves', rendu: (l) => (l.reserves ? `${l.reserves}${l.reservesLevees ? ' ✔' : ''}` : '—') },
          { titre: 'Statut', rendu: (l) => <StatutChip statut={l.statut} /> },
          {
            titre: '', rendu: (l) => adv && (
              <Stack direction="row" spacing={1}>
                <Button size="small" onClick={() => dDate.ouvrir(l, { d: l.datePrevue })}>Date</Button>
                <Button size="small" onClick={() => dPre.ouvrir(l, { date: aujourdhui() })}>Pré-réception</Button>
                {l.reserves && !l.reservesLevees && <Button size="small" onClick={() => lever.mutate(l.id)}>Lever réserves</Button>}
                {l.statut === 'PRE_RECEPTION_FAITE' && <Button size="small" variant="contained" onClick={() => dPv.ouvrir(l, { dateLivraison: aujourdhui(), nombreCles: 3 })}>PV & clés</Button>}
              </Stack>
            ),
          },
        ]} />
      <Typography variant="subtitle1" fontWeight={600} gutterBottom sx={{ mt: 3 }}>Lots livrés</Typography>
      <DataTable cle={(l) => l.id} lignes={livrees.data} chargement={livrees.isLoading} vide="Aucun lot livré"
        colonnes={[
          { titre: 'Livré le', rendu: (l) => date(l.dateLivraison) },
          { titre: 'Dossier', rendu: (l) => l.reservationNumero },
          { titre: 'Client', rendu: (l) => l.clientNom },
          { titre: 'Lot', rendu: (l) => l.lotCode },
          { titre: 'Clés', rendu: (l) => l.nombreCles, align: 'center' },
          { titre: 'Fin SAV', rendu: (l) => date(l.finGarantieSav) },
          { titre: '', rendu: (l) => <Button size="small" onClick={() => ouvrirDocument(`/api/postvente/livraisons/${l.id}/pv`).catch((e: Error) => notifier(e.message, 'error'))}>PV</Button> },
        ]} />
      <FormDialog ouvert={dPlan.ouvert} titre="Planifier une livraison" initial={dPlan.initial} onFermer={dPlan.fermer}
        texte="Seuls les dossiers dont l'acte définitif est signé peuvent être livrés."
        champs={[{ nom: 'reservationId', label: 'Dossier (acte signé)', type: 'select', requis: true,
          options: (actes.data ?? []).map((d) => ({ valeur: d.reservationId, label: `${d.reservationNumero} · ${d.clientNom} · ${d.lotCode}` })) },
          { nom: 'datePrevue', label: 'Date prévue', type: 'date', requis: true }]}
        onValider={(v) => planifier.mutate(v, { onSuccess: dPlan.fermer })} />
      <FormDialog ouvert={dDate.ouvert} titre="Replanifier" initial={dDate.initial} onFermer={dDate.fermer}
        champs={[{ nom: 'd', label: 'Nouvelle date', type: 'date', requis: true }]}
        onValider={(v) => replanifier.mutate({ id: dDate.cible!.id, d: v.d }, { onSuccess: dDate.fermer })} />
      <FormDialog ouvert={dPre.ouvert} titre="Pré-réception avec le client" initial={dPre.initial} onFermer={dPre.fermer}
        champs={[{ nom: 'date', label: 'Date', type: 'date', requis: true }, { nom: 'reserves', label: 'Réserves (finitions à reprendre)', type: 'textarea' }]}
        onValider={(v) => pre.mutate({ id: dPre.cible!.id, corps: v }, { onSuccess: dPre.fermer })} />
      <FormDialog ouvert={dPv.ouvert} titre={`PV de livraison — ${dPv.cible?.lotCode ?? ''}`} initial={dPv.initial} onFermer={dPv.fermer}
        texte="Le solde du dossier sera vérifié : remise des clés refusée s'il reste un montant dû." libelleValider="Livrer et remettre les clés"
        champs={[{ nom: 'dateLivraison', label: 'Date de livraison', type: 'date', requis: true }, { nom: 'nombreCles', label: 'Nombre de clés', type: 'number', requis: true }]}
        onValider={(v) => pv.mutate({ id: dPv.cible!.id, corps: v }, { onSuccess: dPv.fermer })} />
    </Box>
  );
}

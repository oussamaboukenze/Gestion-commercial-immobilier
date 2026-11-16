import { Box, Button, Divider, Drawer, MenuItem, Pagination, Stack, TextField, Typography } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { api, qs } from '../../api/client';
import type { Page, Projet, Prospect, RelanceCrm, RendezVous } from '../../api/types';
import { SOURCES, TYPOLOGIES } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import DataTable from '../../components/DataTable';
import { useDialogue } from '../../components/Dialogue';
import FormDialog, { type Champ } from '../../components/FormDialog';
import PageHeader from '../../components/PageHeader';
import StatutChip from '../../components/StatutChip';
import { useAction } from '../../components/useAction';
import { date, dateHeure, dh, libelle } from '../../format';

const STATUTS = ['NOUVEAU', 'EN_COURS', 'CONVERTI', 'PERDU'];

function FicheProspect({ prospect, onFermer }: { prospect: Prospect; onFermer: () => void }) {
  const { hasRole } = useAuth();
  const ecriture = hasRole('COMMERCIAL', 'RESP_COMMERCIAL');
  const rdv = useQuery({ queryKey: ['rdv-prospect', prospect.id], enabled: ecriture,
    queryFn: () => api.get<RendezVous[]>(`/api/crm/prospects/${prospect.id}/rendez-vous`) });
  const relances = useQuery({ queryKey: ['relances-prospect', prospect.id], enabled: ecriture,
    queryFn: () => api.get<RelanceCrm[]>(`/api/crm/prospects/${prospect.id}/relances`) });
  const dRdv = useDialogue();
  const dRelance = useDialogue();
  const dPerdu = useDialogue();
  const planifier = useAction((v: Record<string, unknown>) => api.post('/api/crm/rendez-vous', { ...v, prospectId: prospect.id }), 'Rendez-vous planifié');
  const relancer = useAction((v: Record<string, unknown>) => api.post('/api/crm/relances', { ...v, prospectId: prospect.id }), 'Relance programmée');
  const perdu = useAction((v: Record<string, unknown>) => api.post(`/api/crm/prospects/${prospect.id}/perdu`, v), 'Prospect marqué perdu');

  return (
    <Box sx={{ width: 460, p: 3 }}>
      <Typography variant="h6">{prospect.prenom} {prospect.nom}</Typography>
      <Stack direction="row" spacing={1} sx={{ my: 1 }}><StatutChip statut={prospect.statut} /><StatutChip statut={prospect.projetCode} /></Stack>
      <Typography variant="body2">📞 {prospect.telephone} · CIN {prospect.cin}</Typography>
      <Typography variant="body2">✉️ {prospect.email || '—'}</Typography>
      <Typography variant="body2">Budget {dh(prospect.budget)} · {prospect.typologieRecherchee} · source {libelle(prospect.source)}</Typography>
      <Typography variant="body2" color="text.secondary">Commercial : {prospect.commercial} · créé le {date(prospect.dateCreation)}</Typography>
      {prospect.motifPerte && <Typography variant="body2" color="error">Perdu : {prospect.motifPerte}</Typography>}
      {ecriture && (
        <>
          <Stack direction="row" spacing={1} sx={{ my: 2 }}>
            <Button size="small" variant="contained" onClick={() => dRdv.ouvrir(undefined, { type: 'VISITE' })}>Rendez-vous</Button>
            <Button size="small" variant="outlined" onClick={() => dRelance.ouvrir()}>Relance</Button>
            {prospect.statut !== 'CONVERTI' && prospect.statut !== 'PERDU' &&
              <Button size="small" color="error" onClick={() => dPerdu.ouvrir()}>Perdu</Button>}
          </Stack>
          <Divider />
          <Typography variant="subtitle2" sx={{ mt: 2 }}>Rendez-vous</Typography>
          {(rdv.data ?? []).map((r) => (
            <Box key={r.id} sx={{ py: 1 }}>
              <Typography variant="body2">{dateHeure(r.dateHeure)} · {libelle(r.type)} <StatutChip statut={r.statut} /></Typography>
              {r.compteRendu && <Typography variant="caption" color="text.secondary">{r.compteRendu}</Typography>}
            </Box>
          ))}
          <Typography variant="subtitle2" sx={{ mt: 2 }}>Relances</Typography>
          {(relances.data ?? []).map((r) => (
            <Typography key={r.id} variant="body2" sx={{ py: 0.5 }}>{date(r.dateRelance)} · {r.objet} {r.faite ? '✔' : ''}</Typography>
          ))}
        </>
      )}
      <Button sx={{ mt: 3 }} onClick={onFermer}>Fermer</Button>
      <FormDialog ouvert={dRdv.ouvert} titre="Planifier un rendez-vous" initial={dRdv.initial} onFermer={dRdv.fermer}
        champs={[{ nom: 'dateHeure', label: 'Date et heure', type: 'datetime-local', requis: true },
          { nom: 'type', label: 'Type', type: 'select', options: ['VISITE', 'RDV_BUREAU', 'APPEL'], requis: true },
          { nom: 'lotId', label: 'Id du lot visité (optionnel)', type: 'number' }]}
        onValider={(v) => planifier.mutate(v, { onSuccess: dRdv.fermer })} />
      <FormDialog ouvert={dRelance.ouvert} titre="Programmer une relance" onFermer={dRelance.fermer}
        champs={[{ nom: 'dateRelance', label: 'Date', type: 'date', requis: true }, { nom: 'objet', label: 'Objet', requis: true }]}
        onValider={(v) => relancer.mutate(v, { onSuccess: dRelance.fermer })} />
      <FormDialog ouvert={dPerdu.ouvert} titre="Marquer le prospect comme perdu" onFermer={dPerdu.fermer} danger libelleValider="Confirmer"
        champs={[{ nom: 'motif', label: 'Motif', type: 'textarea', requis: true }]}
        onValider={(v) => perdu.mutate(v, { onSuccess: () => { dPerdu.fermer(); onFermer(); } })} />
    </Box>
  );
}

export default function Prospects() {
  const { hasRole } = useAuth();
  const ecriture = hasRole('COMMERCIAL', 'RESP_COMMERCIAL');
  const [q, setQ] = useState('');
  const [statut, setStatut] = useState('');
  const [page, setPage] = useState(0);
  const [fiche, setFiche] = useState<Prospect | null>(null);
  const projets = useQuery({ queryKey: ['projets'], queryFn: () => api.get<Projet[]>('/api/referentiel/projets') });
  const prospects = useQuery({
    queryKey: ['prospects', q, statut, page],
    queryFn: () => api.get<Page<Prospect>>('/api/crm/prospects' + qs({ q, statut, page, size: 20 })),
  });
  const dEdition = useDialogue<Prospect>();
  const enregistrer = useAction((v: { id?: number; corps: Record<string, unknown> }) =>
    v.id ? api.put(`/api/crm/prospects/${v.id}`, v.corps) : api.post('/api/crm/prospects', v.corps), 'Prospect enregistré');

  const champs: Champ[] = [
    { nom: 'nom', label: 'Nom', requis: true }, { nom: 'prenom', label: 'Prénom' },
    { nom: 'telephone', label: 'Téléphone', requis: true }, { nom: 'cin', label: 'CIN', requis: true, aide: 'ex. BE123456' },
    { nom: 'email', label: 'Email', type: 'email' },
    { nom: 'source', label: 'Source', type: 'select', options: SOURCES, requis: true },
    { nom: 'budget', label: 'Budget (DH)', type: 'number', requis: true },
    { nom: 'typologieRecherchee', label: 'Typologie recherchée', type: 'select', options: TYPOLOGIES, requis: true },
    { nom: 'projetCode', label: 'Projet', type: 'select', requis: true, options: (projets.data ?? []).map((p) => ({ valeur: p.code, label: `${p.code} · ${p.nom}` })) },
  ];

  return (
    <Box>
      <PageHeader titre="Prospects" sousTitre="CRM > Prospects"
        actions={ecriture && <Button variant="contained" startIcon={<AddIcon />} onClick={() => dEdition.ouvrir()}>Nouveau</Button>} />
      <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
        <TextField size="small" label="Rechercher (nom, téléphone, CIN)" value={q} onChange={(e) => { setQ(e.target.value); setPage(0); }} sx={{ width: 320 }} />
        <TextField select size="small" label="Statut" value={statut} onChange={(e) => { setStatut(e.target.value); setPage(0); }} sx={{ minWidth: 160 }}>
          <MenuItem value="">Tous</MenuItem>
          {STATUTS.map((s) => <MenuItem key={s} value={s}>{libelle(s)}</MenuItem>)}
        </TextField>
      </Stack>
      <DataTable cle={(p) => p.id} lignes={prospects.data?.content} chargement={prospects.isLoading} onClick={setFiche}
        colonnes={[
          { titre: 'Nom', rendu: (p) => <b>{p.prenom} {p.nom}</b> },
          { titre: 'Téléphone', rendu: (p) => p.telephone },
          { titre: 'CIN', rendu: (p) => p.cin },
          { titre: 'Projet', rendu: (p) => p.projetCode },
          { titre: 'Typologie', rendu: (p) => p.typologieRecherchee },
          { titre: 'Budget', rendu: (p) => dh(p.budget), align: 'right' },
          { titre: 'Commercial', rendu: (p) => p.commercial },
          { titre: 'Statut', rendu: (p) => <StatutChip statut={p.statut} /> },
          { titre: '', rendu: (p) => ecriture && <Button size="small" onClick={(e) => { e.stopPropagation(); dEdition.ouvrir(p, { ...p }); }}>Modifier</Button> },
        ]} />
      {(prospects.data?.page.totalPages ?? 0) > 1 &&
        <Pagination sx={{ mt: 2 }} count={prospects.data!.page.totalPages} page={page + 1} onChange={(_, p) => setPage(p - 1)} />}
      <Drawer anchor="right" open={!!fiche} onClose={() => setFiche(null)}>
        {fiche && <FicheProspect prospect={fiche} onFermer={() => setFiche(null)} />}
      </Drawer>
      <FormDialog ouvert={dEdition.ouvert} titre={dEdition.cible ? 'Modifier le prospect' : 'Nouveau prospect'}
        initial={dEdition.initial} champs={champs} onFermer={dEdition.fermer} enCours={enregistrer.isPending}
        onValider={(v) => enregistrer.mutate({ id: dEdition.cible?.id, corps: v }, { onSuccess: dEdition.fermer })} />
    </Box>
  );
}

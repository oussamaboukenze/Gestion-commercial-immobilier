import {
  Accordion, AccordionDetails, AccordionSummary, Box, Button, MenuItem, Pagination, Stack, Tab, Tabs, TextField,
  Typography,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import AddIcon from '@mui/icons-material/Add';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, qs } from '../../api/client';
import type { Immeuble, Lot, Page, Projet, Prospect, Tranche } from '../../api/types';
import { LOT_STATUTS, TYPOLOGIES } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import DataTable from '../../components/DataTable';
import { useDialogue } from '../../components/Dialogue';
import FormDialog from '../../components/FormDialog';
import PageHeader from '../../components/PageHeader';
import StatutChip from '../../components/StatutChip';
import { useAction } from '../../components/useAction';
import { dh, libelle } from '../../format';

function Structure({ projet }: { projet: string }) {
  const { hasRole } = useAuth();
  const admin = hasRole('ADMIN_SI');
  const tranches = useQuery({ queryKey: ['tranches', projet], queryFn: () => api.get<Tranche[]>(`/api/referentiel/projets/${projet}/tranches`) });
  const dTranche = useDialogue();
  const dImmeuble = useDialogue<number>();
  const dLot = useDialogue<number>();
  const creerTranche = useAction((v: Record<string, unknown>) => api.post(`/api/referentiel/projets/${projet}/tranches`, v), 'Tranche créée');
  const creerImmeuble = useAction((v: { id: number; corps: Record<string, unknown> }) => api.post(`/api/referentiel/tranches/${v.id}/immeubles`, v.corps), 'Immeuble créé');
  const creerLot = useAction((v: { id: number; corps: Record<string, unknown> }) => api.post(`/api/referentiel/immeubles/${v.id}/lots`, v.corps), 'Lot créé');

  return (
    <Box sx={{ mb: 3 }}>
      <Stack direction="row" justifyContent="space-between" sx={{ mb: 1 }}>
        <Typography variant="subtitle1" fontWeight={600}>Structure du projet {projet}</Typography>
        {admin && <Button size="small" startIcon={<AddIcon />} onClick={() => dTranche.ouvrir()}>Tranche</Button>}
      </Stack>
      {(tranches.data ?? []).map((t) => (
        <TrancheBloc key={t.id} tranche={t} admin={admin} onImmeuble={() => dImmeuble.ouvrir(t.id)} onLot={(id) => dLot.ouvrir(id)} />
      ))}
      <FormDialog ouvert={dTranche.ouvert} titre="Nouvelle tranche" onFermer={dTranche.fermer} enCours={creerTranche.isPending}
        champs={[{ nom: 'code', label: 'Code (ex. T2)', requis: true }, { nom: 'nom', label: 'Nom' }]}
        onValider={(v) => creerTranche.mutate(v, { onSuccess: dTranche.fermer })} />
      <FormDialog ouvert={dImmeuble.ouvert} titre="Nouvel immeuble / îlot" onFermer={dImmeuble.fermer} enCours={creerImmeuble.isPending}
        initial={{ type: 'IMMEUBLE' }}
        champs={[{ nom: 'code', label: 'Code (ex. IMM04)', requis: true }, { nom: 'type', label: 'Type', type: 'select', options: ['IMMEUBLE', 'ILOT'], requis: true },
          { nom: 'nombreEtages', label: "Nombre d'étages", type: 'number' }]}
        onValider={(v) => creerImmeuble.mutate({ id: dImmeuble.cible!, corps: v }, { onSuccess: dImmeuble.fermer })} />
      <FormDialog ouvert={dLot.ouvert} titre="Nouveau lot" onFermer={dLot.fermer} enCours={creerLot.isPending}
        texte="Le prix initial doit correspondre à la grille validée par la direction."
        champs={[{ nom: 'numero', label: 'Numéro (ex. A12)', requis: true }, { nom: 'etage', label: 'Etage', type: 'number', requis: true },
          { nom: 'typologie', label: 'Typologie', type: 'select', options: TYPOLOGIES, requis: true },
          { nom: 'surface', label: 'Surface (m²)', type: 'number' }, { nom: 'prixTTC', label: 'Prix TTC (DH)', type: 'number', requis: true }]}
        onValider={(v) => creerLot.mutate({ id: dLot.cible!, corps: v }, { onSuccess: dLot.fermer })} />
    </Box>
  );
}

function TrancheBloc({ tranche, admin, onImmeuble, onLot }: { tranche: Tranche; admin: boolean; onImmeuble: () => void; onLot: (immeubleId: number) => void }) {
  const [ouvert, setOuvert] = useState(false);
  const immeubles = useQuery({
    queryKey: ['immeubles', tranche.id], enabled: ouvert,
    queryFn: () => api.get<Immeuble[]>(`/api/referentiel/tranches/${tranche.id}/immeubles`),
  });
  return (
    <Accordion expanded={ouvert} onChange={() => setOuvert(!ouvert)} disableGutters variant="outlined">
      <AccordionSummary expandIcon={<ExpandMoreIcon />}>
        <Typography>{tranche.code} {tranche.nom ? `— ${tranche.nom}` : ''}</Typography>
      </AccordionSummary>
      <AccordionDetails>
        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
          {(immeubles.data ?? []).map((i) => (
            <Button key={i.id} size="small" variant="outlined" onClick={() => admin && onLot(i.id)} disabled={!admin}>
              {libelle(i.type)} {i.code} {admin ? '· + lot' : ''}
            </Button>
          ))}
          {admin && <Button size="small" startIcon={<AddIcon />} onClick={onImmeuble}>Immeuble / îlot</Button>}
          {immeubles.data?.length === 0 && !admin && <Typography variant="body2" color="text.secondary">Aucun immeuble</Typography>}
        </Stack>
      </AccordionDetails>
    </Accordion>
  );
}

export default function ProjetsLots() {
  const { hasRole } = useAuth();
  const navigate = useNavigate();
  const commercial = hasRole('COMMERCIAL', 'RESP_COMMERCIAL');
  const admin = hasRole('ADMIN_SI');
  const [projet, setProjet] = useState<string>('');
  const [typologie, setTypologie] = useState('');
  const [statut, setStatut] = useState('');
  const [page, setPage] = useState(0);

  const projets = useQuery({ queryKey: ['projets'], queryFn: () => api.get<Projet[]>('/api/referentiel/projets') });
  const projetActif = projet || projets.data?.[0]?.code || '';
  const lots = useQuery({
    queryKey: ['lots', projetActif, typologie, statut, page], enabled: !!projetActif,
    queryFn: () => api.get<Page<Lot>>('/api/referentiel/lots' + qs({ projet: projetActif, typologie, statut, page, size: 20, sort: 'code' })),
  });
  const prospects = useQuery({
    queryKey: ['prospects-select', projetActif], enabled: commercial && !!projetActif,
    queryFn: () => api.get<Page<Prospect>>('/api/crm/prospects' + qs({ projet: projetActif, size: 200 })),
  });

  const dProjet = useDialogue();
  const dOption = useDialogue<Lot>();
  const dPrix = useDialogue<Lot>();
  const creerProjet = useAction((v: Record<string, unknown>) => api.post('/api/referentiel/projets', v), 'Projet créé');
  const option = useAction((v: { lotId: number; prospectId: unknown }) => api.post('/api/ventes/options', v), 'Lot mis en option pour 48 h');
  const demandePrix = useAction((v: { id: number; corps: Record<string, unknown> }) => api.post(`/api/referentiel/lots/${v.id}/demandes-prix`, v.corps),
    'Demande envoyée à la direction');

  const optionsProspects = (prospects.data?.content ?? []).filter((p) => p.statut !== 'PERDU')
    .map((p) => ({ valeur: p.id, label: `${p.prenom ?? ''} ${p.nom} — ${p.cin}` }));

  return (
    <Box>
      <PageHeader titre="Projets & lots" sousTitre="Référentiel > Lots : stock en temps réel"
        actions={admin && <Button variant="contained" startIcon={<AddIcon />} onClick={() => dProjet.ouvrir(undefined, { statut: 'PRE_COMMERCIALISATION' })}>Projet</Button>} />
      <Tabs value={projetActif} onChange={(_, v) => { setProjet(v); setPage(0); }} sx={{ mb: 2 }} variant="scrollable">
        {(projets.data ?? []).map((p) => <Tab key={p.code} value={p.code} label={`${p.code} · ${p.nom}`} />)}
      </Tabs>
      {projetActif && (admin || hasRole('DIRECTION')) && <Structure projet={projetActif} />}

      <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
        <TextField select size="small" label="Typologie" value={typologie} onChange={(e) => { setTypologie(e.target.value); setPage(0); }} sx={{ minWidth: 160 }}>
          <MenuItem value="">Toutes</MenuItem>
          {TYPOLOGIES.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
        </TextField>
        <TextField select size="small" label="Statut" value={statut} onChange={(e) => { setStatut(e.target.value); setPage(0); }} sx={{ minWidth: 180 }}>
          <MenuItem value="">Tous</MenuItem>
          {LOT_STATUTS.map((s) => <MenuItem key={s} value={s}>{libelle(s)}</MenuItem>)}
        </TextField>
      </Stack>

      <DataTable cle={(l) => l.id} lignes={lots.data?.content} chargement={lots.isLoading}
        colonnes={[
          { titre: 'Code', rendu: (l) => <b>{l.code}</b> },
          { titre: 'Typologie', rendu: (l) => l.typologie },
          { titre: 'Etage', rendu: (l) => l.etage, align: 'center' },
          { titre: 'Surface', rendu: (l) => (l.surface ? `${l.surface} m²` : '—'), align: 'right' },
          { titre: 'Prix TTC', rendu: (l) => dh(l.prixTTC), align: 'right' },
          { titre: 'Statut', rendu: (l) => <StatutChip statut={l.statut} /> },
          {
            titre: 'Actions', rendu: (l) => (
              <Stack direction="row" spacing={1}>
                {commercial && l.statut === 'DISPONIBLE' && <Button size="small" onClick={() => dOption.ouvrir(l)}>Option</Button>}
                {commercial && (l.statut === 'DISPONIBLE' || l.statut === 'OPTION') &&
                  <Button size="small" variant="outlined" onClick={() => navigate(`/ventes/reservations?nouveau=${l.id}`)}>Réserver</Button>}
                {admin && (l.statut === 'DISPONIBLE' || l.statut === 'OPTION') &&
                  <Button size="small" onClick={() => dPrix.ouvrir(l, { nouveauPrix: l.prixTTC })}>Prix</Button>}
              </Stack>
            ),
          },
        ]} />
      {(lots.data?.page.totalPages ?? 0) > 1 && (
        <Pagination sx={{ mt: 2 }} count={lots.data!.page.totalPages} page={page + 1} onChange={(_, p) => setPage(p - 1)} />
      )}

      <FormDialog ouvert={dProjet.ouvert} titre="Nouveau projet" onFermer={dProjet.fermer} initial={dProjet.initial} enCours={creerProjet.isPending}
        champs={[{ nom: 'code', label: 'Code (ex. RNA)', requis: true }, { nom: 'nom', label: 'Nom', requis: true },
          { nom: 'ville', label: 'Ville', requis: true }, { nom: 'type', label: 'Type (ex. Moyen standing)' },
          { nom: 'statut', label: 'Statut', type: 'select', requis: true, options: ['PRE_COMMERCIALISATION', 'COMMERCIALISATION', 'LIVRAISON', 'CLOTURE'] }]}
        onValider={(v) => creerProjet.mutate(v, { onSuccess: dProjet.fermer })} />
      <FormDialog ouvert={dOption.ouvert} titre={`Mettre le lot ${dOption.cible?.code ?? ''} en option`} onFermer={dOption.fermer}
        texte="Blocage 48 h maximum, 1 option active par prospect." libelleValider="Bloquer 48 h" enCours={option.isPending}
        champs={[{ nom: 'prospectId', label: 'Prospect', type: 'select', requis: true, options: optionsProspects }]}
        onValider={(v) => option.mutate({ lotId: dOption.cible!.id, prospectId: v.prospectId }, { onSuccess: dOption.fermer })} />
      <FormDialog ouvert={dPrix.ouvert} titre={`Modifier le prix de ${dPrix.cible?.code ?? ''}`} onFermer={dPrix.fermer} initial={dPrix.initial}
        texte="La modification sera appliquée après validation de la direction." libelleValider="Envoyer à la direction" enCours={demandePrix.isPending}
        champs={[{ nom: 'nouveauPrix', label: 'Nouveau prix TTC', type: 'number', requis: true }, { nom: 'motif', label: 'Motif', type: 'textarea', requis: true }]}
        onValider={(v) => demandePrix.mutate({ id: dPrix.cible!.id, corps: v }, { onSuccess: dPrix.fermer })} />
    </Box>
  );
}

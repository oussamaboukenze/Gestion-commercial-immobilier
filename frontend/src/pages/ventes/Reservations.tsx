import { Box, Button, MenuItem, Pagination, Stack, TextField } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { api, qs } from '../../api/client';
import type { Lot, Page, Prospect, Reservation } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import DataTable from '../../components/DataTable';
import { useDialogue } from '../../components/Dialogue';
import FormDialog from '../../components/FormDialog';
import PageHeader from '../../components/PageHeader';
import StatutChip from '../../components/StatutChip';
import { useAction } from '../../components/useAction';
import { date, dh, libelle } from '../../format';

const STATUTS = ['EN_ATTENTE_VALIDATION_ADV', 'VALIDEE', 'COMPROMIS_SIGNE', 'REJETEE', 'DESISTEE'];

export default function Reservations() {
  const { hasRole } = useAuth();
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const commercial = hasRole('COMMERCIAL', 'RESP_COMMERCIAL');
  const [statut, setStatut] = useState(hasRole('ADV') ? 'EN_ATTENTE_VALIDATION_ADV' : '');
  const [page, setPage] = useState(0);
  const reservations = useQuery({
    queryKey: ['reservations', statut, page],
    queryFn: () => api.get<Page<Reservation>>('/api/ventes/reservations' + qs({ statut, page, size: 20 })),
  });

  // Création : lot choisi depuis Projets & lots (?nouveau=<lotId>)
  const dNouveau = useDialogue<Lot>();
  const lotId = params.get('nouveau');
  const lot = useQuery({ queryKey: ['lot', lotId], enabled: !!lotId, queryFn: () => api.get<Lot>(`/api/referentiel/lots/${lotId}`) });
  useEffect(() => {
    if (lot.data) dNouveau.ouvrir(lot.data, { remisePct: 0, avance: Math.ceil(lot.data.prixTTC * 0.05) });
  }, [lot.data]); // eslint-disable-line react-hooks/exhaustive-deps
  const prospects = useQuery({
    queryKey: ['prospects-select', dNouveau.cible?.projetCode], enabled: !!dNouveau.cible,
    queryFn: () => api.get<Page<Prospect>>('/api/crm/prospects' + qs({ projet: dNouveau.cible!.projetCode, size: 200 })),
  });
  const creer = useAction((corps: Record<string, unknown>) => api.post<Reservation>('/api/ventes/reservations', corps),
    'Réservation créée : en attente de validation ADV');
  const fermer = () => { dNouveau.fermer(); setParams({}); };

  return (
    <Box>
      <PageHeader titre="Réservations" sousTitre="Ventes > Réservations"
        actions={commercial && <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/referentiel')}>Choisir un lot</Button>} />
      <TextField select size="small" label="Statut" value={statut} onChange={(e) => { setStatut(e.target.value); setPage(0); }} sx={{ mb: 2, minWidth: 260 }}>
        <MenuItem value="">Tous</MenuItem>
        {STATUTS.map((s) => <MenuItem key={s} value={s}>{libelle(s)}</MenuItem>)}
      </TextField>
      <DataTable cle={(r) => r.id} lignes={reservations.data?.content} chargement={reservations.isLoading}
        onClick={(r) => navigate(`/ventes/reservations/${r.id}`)}
        colonnes={[
          { titre: 'N°', rendu: (r) => <b>{r.numero}</b> },
          { titre: 'Client', rendu: (r) => r.clientNom },
          { titre: 'Lot', rendu: (r) => r.lotCode },
          { titre: 'Prix net', rendu: (r) => dh(r.prixNet), align: 'right' },
          { titre: 'Encaissé', rendu: (r) => dh(r.totalEncaisse), align: 'right' },
          { titre: 'Remise', rendu: (r) => (r.remisePct > 0 ? <>{r.remisePct} % <StatutChip statut={r.statutRemise} /></> : '—') },
          { titre: 'Commercial', rendu: (r) => r.commercial },
          { titre: 'Créée le', rendu: (r) => date(r.dateCreation) },
          { titre: 'Statut', rendu: (r) => <StatutChip statut={r.statut} /> },
        ]} />
      {(reservations.data?.page.totalPages ?? 0) > 1 &&
        <Pagination sx={{ mt: 2 }} count={reservations.data!.page.totalPages} page={page + 1} onChange={(_, p) => setPage(p - 1)} />}

      <FormDialog ouvert={dNouveau.ouvert} titre={`Nouvelle réservation — lot ${dNouveau.cible?.code ?? ''}`} initial={dNouveau.initial}
        onFermer={fermer} enCours={creer.isPending} libelleValider="Créer la réservation"
        texte={dNouveau.cible && (
          <Stack spacing={0.5}>
            <span>Prix TTC : <b>{dh(dNouveau.cible.prixTTC)}</b> · avance minimum 5 % du prix net.</span>
            <span>Remise : jusqu'à 2 % sans validation, 2–5 % responsable commercial, au-delà direction.</span>
          </Stack>
        )}
        champs={[
          { nom: 'prospectId', label: 'Client (prospect)', type: 'select', requis: true,
            options: (prospects.data?.content ?? []).filter((p) => p.statut !== 'PERDU').map((p) => ({ valeur: p.id, label: `${p.prenom ?? ''} ${p.nom} — ${p.cin}` })) },
          { nom: 'remisePct', label: 'Remise (%)', type: 'number' },
          { nom: 'avance', label: 'Avance (DH)', type: 'number', requis: true },
          { nom: 'pieceCin', label: 'CIN fournie', type: 'checkbox' },
          { nom: 'pieceJustificatifRevenus', label: 'Justificatif de revenus fourni', type: 'checkbox' },
          { nom: 'pieceRecuAvance', label: "Reçu de l'avance fourni", type: 'checkbox' },
        ]}
        onValider={(v) => creer.mutate({ ...v, lotId: dNouveau.cible!.id },
          { onSuccess: (r) => { fermer(); navigate(`/ventes/reservations/${r.id}`); } })} />
    </Box>
  );
}

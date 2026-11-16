import { Box, Pagination, TextField, Tooltip, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { api, qs } from '../../api/client';
import type { Journal, Page } from '../../api/types';
import DataTable from '../../components/DataTable';
import PageHeader from '../../components/PageHeader';
import { dateHeure } from '../../format';

/** Administration > Journal d'activité (tous les événements métier). */
export default function JournalPage() {
  const [evenement, setEvenement] = useState('');
  const [page, setPage] = useState(0);
  const journal = useQuery({
    queryKey: ['journal', evenement, page],
    queryFn: () => api.get<Page<Journal>>('/api/admin/journal' + qs({ evenement, page, size: 50, sort: 'date,desc' })),
  });
  return (
    <Box>
      <PageHeader titre="Journal d'activité" sousTitre="Réservations, encaissements, changements de statut, administration..." />
      <TextField size="small" label="Filtrer (ex. reservation, encaissement, admin)" value={evenement}
        onChange={(e) => { setEvenement(e.target.value); setPage(0); }} sx={{ mb: 2, width: 360 }} />
      <DataTable cle={(j) => j.id} lignes={journal.data?.content} chargement={journal.isLoading}
        colonnes={[
          { titre: 'Date', rendu: (j) => dateHeure(j.date), largeur: 150 },
          { titre: 'Evénement', rendu: (j) => <b>{j.evenement}</b>, largeur: 220 },
          {
            titre: 'Détails', rendu: (j) => (
              <Tooltip title={j.details}><Typography variant="body2" noWrap sx={{ maxWidth: 700, fontFamily: 'monospace', fontSize: 12 }}>{j.details}</Typography></Tooltip>
            ),
          },
        ]} />
      {(journal.data?.page.totalPages ?? 0) > 1 &&
        <Pagination sx={{ mt: 2 }} count={journal.data!.page.totalPages} page={page + 1} onChange={(_, p) => setPage(p - 1)} />}
    </Box>
  );
}

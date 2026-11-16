import { Box, Button, Stack } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../api/client';
import type { DemandePrix } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import DataTable from '../../components/DataTable';
import PageHeader from '../../components/PageHeader';
import { useAction } from '../../components/useAction';
import { dateHeure, dh } from '../../format';

/** Référentiel > Grilles de prix : demandes de l'admin SI, validation par la direction. */
export default function GrillesPrix() {
  const { hasRole } = useAuth();
  const direction = hasRole('DIRECTION');
  const demandes = useQuery({ queryKey: ['demandes-prix'], queryFn: () => api.get<DemandePrix[]>('/api/referentiel/demandes-prix') });
  const decider = useAction((v: { id: number; ok: boolean }) => api.post(`/api/referentiel/demandes-prix/${v.id}/${v.ok ? 'valider' : 'refuser'}`), 'Décision enregistrée');
  return (
    <Box>
      <PageHeader titre="Grilles de prix" sousTitre="Modification de prix : initiée par l'admin SI, validée par la direction (depuis Projets & lots > Prix)" />
      <DataTable cle={(d) => d.id} lignes={demandes.data} chargement={demandes.isLoading} vide="Aucune demande en attente"
        colonnes={[
          { titre: 'Lot', rendu: (d) => <b>{d.lotCode}</b> },
          { titre: 'Ancien prix', rendu: (d) => dh(d.ancienPrix), align: 'right' },
          { titre: 'Nouveau prix', rendu: (d) => dh(d.nouveauPrix), align: 'right' },
          { titre: 'Ecart', rendu: (d) => `${(((d.nouveauPrix - d.ancienPrix) / d.ancienPrix) * 100).toFixed(1)} %`, align: 'right' },
          { titre: 'Motif', rendu: (d) => d.motif },
          { titre: 'Demandé par', rendu: (d) => `${d.demandeur} · ${dateHeure(d.dateDemande)}` },
          {
            titre: '', rendu: (d) => direction && (
              <Stack direction="row" spacing={1}>
                <Button size="small" variant="contained" onClick={() => decider.mutate({ id: d.id, ok: true })}>Valider</Button>
                <Button size="small" color="error" onClick={() => decider.mutate({ id: d.id, ok: false })}>Refuser</Button>
              </Stack>
            ),
          },
        ]} />
    </Box>
  );
}

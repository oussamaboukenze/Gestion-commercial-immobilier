import { Box, Button, Stack } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../api/client';
import type { Correction } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import DataTable from '../../components/DataTable';
import PageHeader from '../../components/PageHeader';
import { useAction } from '../../components/useAction';
import { dateHeure, dh } from '../../format';

/** Corrections d'encaissement (contre-passation) : validation DAF. */
export default function Corrections() {
  const { hasRole } = useAuth();
  const daf = hasRole('DAF');
  const liste = useQuery({ queryKey: ['corrections'], queryFn: () => api.get<Correction[]>('/api/tresorerie/corrections/en-attente') });
  const decider = useAction((v: { id: number; ok: boolean }) => api.post(`/api/tresorerie/corrections/${v.id}/${v.ok ? 'valider' : 'refuser'}`), 'Décision enregistrée');
  return (
    <Box>
      <PageHeader titre="Corrections d'encaissement" sousTitre="Demandées par la comptabilité depuis Encaissements > Contre-passer" />
      <DataTable cle={(c) => c.id} lignes={liste.data} chargement={liste.isLoading} vide="Aucune correction en attente"
        colonnes={[
          { titre: 'Reçu', rendu: (c) => c.numeroRecu },
          { titre: 'Client', rendu: (c) => c.clientNom },
          { titre: 'Montant', rendu: (c) => dh(c.montant), align: 'right' },
          { titre: 'Motif', rendu: (c) => c.motif },
          { titre: 'Demandé par', rendu: (c) => `${c.demandeur} · ${dateHeure(c.dateDemande)}` },
          {
            titre: '', rendu: (c) => daf && (
              <Stack direction="row" spacing={1}>
                <Button size="small" variant="contained" onClick={() => decider.mutate({ id: c.id, ok: true })}>Valider</Button>
                <Button size="small" color="error" onClick={() => decider.mutate({ id: c.id, ok: false })}>Refuser</Button>
              </Stack>
            ),
          },
        ]} />
    </Box>
  );
}

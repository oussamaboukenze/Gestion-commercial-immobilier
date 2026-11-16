import { Box, Button, Dialog, DialogContent, DialogTitle, Stack } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { api } from '../../api/client';
import type { RelanceClient } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import DataTable from '../../components/DataTable';
import PageHeader from '../../components/PageHeader';
import StatutChip from '../../components/StatutChip';
import { useAction } from '../../components/useAction';
import { useNotifier } from '../../components/Notifier';
import { date, dh, libelle } from '../../format';

/** Trésorerie > Relances : 1re J+7, 2e J+15, mise en demeure J+30 (validée par le DAF). */
export default function Relances() {
  const { hasRole } = useAuth();
  const notifier = useNotifier();
  const [lettre, setLettre] = useState<string | null>(null);
  const relances = useQuery({ queryKey: ['relances-clients'], queryFn: () => api.get<RelanceClient[]>('/api/tresorerie/relances') });
  const validerDaf = useAction((id: number) => api.post(`/api/tresorerie/relances/${id}/valider-daf`), 'Mise en demeure validée');
  const envoyee = useAction((id: number) => api.post(`/api/tresorerie/relances/${id}/envoyee`), 'Relance marquée envoyée');
  const annuler = useAction((id: number) => api.post(`/api/tresorerie/relances/${id}/annuler`), 'Relance annulée');
  const voirLettre = (id: number) => api.get<string>(`/api/tresorerie/relances/${id}/lettre`).then(setLettre).catch((e: Error) => notifier(e.message, 'error'));

  return (
    <Box>
      <PageHeader titre="Relances clients" sousTitre="Générées automatiquement chaque matin à partir des échéances en retard" />
      <DataTable cle={(r) => r.id} lignes={relances.data} chargement={relances.isLoading} vide="Aucune relance à traiter"
        colonnes={[
          { titre: 'Niveau', rendu: (r) => <b>{libelle(r.niveau)}</b> },
          { titre: 'Dossier', rendu: (r) => r.reservationNumero },
          { titre: 'Client', rendu: (r) => r.clientNom },
          { titre: 'Objet', rendu: (r) => r.objet },
          { titre: 'Montant dû', rendu: (r) => dh(r.montantDu), align: 'right' },
          { titre: 'Echéance', rendu: (r) => date(r.dateEcheance) },
          { titre: 'Statut', rendu: (r) => <StatutChip statut={r.statut} /> },
          {
            titre: '', rendu: (r) => (
              <Stack direction="row" spacing={1}>
                {r.statut === 'EN_ATTENTE_VALIDATION_DAF' && hasRole('DAF') &&
                  <Button size="small" variant="contained" onClick={() => validerDaf.mutate(r.id)}>Valider (DAF)</Button>}
                {r.statut === 'A_ENVOYER' && <Button size="small" onClick={() => voirLettre(r.id)}>Lettre</Button>}
                {r.statut === 'A_ENVOYER' && hasRole('COMPTABLE') &&
                  <Button size="small" variant="outlined" onClick={() => envoyee.mutate(r.id)}>Envoyée</Button>}
                <Button size="small" color="error" onClick={() => annuler.mutate(r.id)}>Annuler</Button>
              </Stack>
            ),
          },
        ]} />
      <Dialog open={!!lettre} onClose={() => setLettre(null)} maxWidth="md" fullWidth>
        <DialogTitle>Lettre de relance</DialogTitle>
        <DialogContent>
          <Box component="pre" sx={{ whiteSpace: 'pre-wrap', fontFamily: 'Georgia, serif', fontSize: 14 }}>{lettre}</Box>
          <Button onClick={() => { const w = window.open(''); w?.document.write(`<pre style="font-family:Georgia;font-size:14px">${(lettre ?? '').replace(/</g, '&lt;')}</pre>`); w?.print(); }}>
            Imprimer
          </Button>
        </DialogContent>
      </Dialog>
    </Box>
  );
}

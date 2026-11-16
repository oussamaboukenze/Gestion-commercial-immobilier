import { Box, Button, Stack, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { api } from '../../api/client';
import type { ChangementLot, Desistement, Reservation } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import DataTable from '../../components/DataTable';
import { useDialogue } from '../../components/Dialogue';
import FormDialog from '../../components/FormDialog';
import PageHeader from '../../components/PageHeader';
import StatutChip from '../../components/StatutChip';
import { useAction } from '../../components/useAction';
import { date, dh } from '../../format';

/** Toutes les validations de la matrice AHI regroupées sur un écran. */
export default function Validations() {
  const { hasRole } = useAuth();
  const navigate = useNavigate();
  const resp = hasRole('RESP_COMMERCIAL');
  const daf = hasRole('DAF');
  const voitRemises = hasRole('RESP_COMMERCIAL', 'DIRECTION');
  const voitChangements = hasRole('ADV', 'RESP_COMMERCIAL', 'DIRECTION');

  const remises = useQuery({ queryKey: ['remises-attente'], enabled: voitRemises,
    queryFn: () => api.get<Reservation[]>('/api/ventes/reservations/remises-en-attente') });
  const desistements = useQuery({ queryKey: ['desistements-attente'],
    queryFn: () => api.get<Desistement[]>('/api/ventes/desistements/en-attente') });
  const changements = useQuery({ queryKey: ['changements-attente'], enabled: voitChangements,
    queryFn: () => api.get<ChangementLot[]>('/api/ventes/changements-lot/en-attente') });

  const remise = useAction((v: { id: number; ok: boolean }) => api.post(`/api/ventes/reservations/${v.id}/remise/${v.ok ? 'accorder' : 'refuser'}`), 'Décision enregistrée');
  const validerDes = useAction((d: Desistement) => api.post(`/api/ventes/desistements/${d.id}/${d.statut === 'EN_ATTENTE_DAF' ? 'valider-daf' : 'valider-resp-commercial'}`), 'Désistement validé');
  const refuserDes = useAction((v: { id: number; motif: unknown }) => api.post(`/api/ventes/desistements/${v.id}/refuser`, { motif: v.motif }), 'Désistement refusé');
  const changement = useAction((v: { id: number; ok: boolean }) => api.post(`/api/ventes/changements-lot/${v.id}/${v.ok ? 'valider' : 'refuser'}`), 'Décision enregistrée');
  const dRefus = useDialogue<Desistement>();

  const peutValiderDes = (d: Desistement) => (d.statut === 'EN_ATTENTE_DAF' ? daf : resp);

  return (
    <Box>
      <PageHeader titre="Validations" sousTitre="Remises, désistements et changements de lot en attente" />
      {voitRemises && (
        <>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>Remises (2–5 % : responsable commercial · &gt; 5 % : direction)</Typography>
          <DataTable cle={(r) => r.id} lignes={remises.data} chargement={remises.isLoading} vide="Aucune remise en attente"
            colonnes={[
              { titre: 'Réservation', rendu: (r) => <Button size="small" onClick={() => navigate(`/ventes/reservations/${r.id}`)}>{r.numero}</Button> },
              { titre: 'Client', rendu: (r) => r.clientNom },
              { titre: 'Lot', rendu: (r) => r.lotCode },
              { titre: 'Prix TTC', rendu: (r) => dh(r.prixTTC), align: 'right' },
              { titre: 'Remise', rendu: (r) => <b>{r.remisePct} %</b> },
              { titre: 'Commercial', rendu: (r) => r.commercial },
              { titre: 'Niveau', rendu: (r) => <StatutChip statut={r.statutRemise} /> },
              {
                titre: '', rendu: (r) => (r.statutRemise === 'EN_ATTENTE_DIRECTION' ? hasRole('DIRECTION') : true) && (
                  <Stack direction="row" spacing={1}>
                    <Button size="small" variant="contained" onClick={() => remise.mutate({ id: r.id, ok: true })}>Accorder</Button>
                    <Button size="small" color="error" onClick={() => remise.mutate({ id: r.id, ok: false })}>Refuser</Button>
                  </Stack>
                ),
              },
            ]} />
        </>
      )}

      <Typography variant="subtitle1" fontWeight={600} gutterBottom sx={{ mt: 3 }}>Désistements (responsable commercial, + DAF après compromis)</Typography>
      <DataTable cle={(d) => d.id} lignes={desistements.data} chargement={desistements.isLoading} vide="Aucun désistement en attente"
        colonnes={[
          { titre: 'Réservation', rendu: (d) => <Button size="small" onClick={() => navigate(`/ventes/reservations/${d.reservationId}`)}>{d.reservationNumero}</Button> },
          { titre: 'Client', rendu: (d) => d.clientNom },
          { titre: 'Motif', rendu: (d) => d.motif },
          { titre: 'Après compromis', rendu: (d) => (d.apresCompromis ? 'Oui' : 'Non') },
          { titre: 'Versé', rendu: (d) => dh(d.totalVerse), align: 'right' },
          { titre: 'Frais', rendu: (d) => dh(d.fraisDossier), align: 'right' },
          { titre: 'A rembourser', rendu: (d) => <b>{dh(d.montantARembourser)}</b>, align: 'right' },
          { titre: 'Statut', rendu: (d) => <StatutChip statut={d.statut} /> },
          {
            titre: '', rendu: (d) => peutValiderDes(d) && (
              <Stack direction="row" spacing={1}>
                <Button size="small" variant="contained" onClick={() => validerDes.mutate(d)}>Valider</Button>
                <Button size="small" color="error" onClick={() => dRefus.ouvrir(d)}>Refuser</Button>
              </Stack>
            ),
          },
        ]} />

      {voitChangements && (
        <>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom sx={{ mt: 3 }}>Changements de lot (responsable commercial)</Typography>
          <DataTable cle={(c) => c.id} lignes={changements.data} chargement={changements.isLoading} vide="Aucun changement en attente"
            colonnes={[
              { titre: 'Réservation', rendu: (c) => c.reservationNumero },
              { titre: 'Client', rendu: (c) => c.clientNom },
              { titre: 'Ancien lot', rendu: (c) => `${c.ancienLotCode} (${dh(c.ancienPrix)})` },
              { titre: 'Nouveau lot', rendu: (c) => `${c.nouveauLotCode} (${dh(c.nouveauPrix)})` },
              { titre: 'Motif', rendu: (c) => c.motif },
              { titre: 'Saisi par', rendu: (c) => c.saisiPar },
              {
                titre: '', rendu: (c) => resp && (
                  <Stack direction="row" spacing={1}>
                    <Button size="small" variant="contained" onClick={() => changement.mutate({ id: c.id, ok: true })}>Valider</Button>
                    <Button size="small" color="error" onClick={() => changement.mutate({ id: c.id, ok: false })}>Refuser</Button>
                  </Stack>
                ),
              },
            ]} />
        </>
      )}
      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 2 }}>Mis à jour le {date(new Date().toISOString())}</Typography>
      <FormDialog ouvert={dRefus.ouvert} titre="Refuser le désistement" onFermer={dRefus.fermer} danger libelleValider="Refuser"
        champs={[{ nom: 'motif', label: 'Motif du refus', type: 'textarea', requis: true }]}
        onValider={(v) => refuserDes.mutate({ id: dRefus.cible!.id, motif: v.motif }, { onSuccess: dRefus.fermer })} />
    </Box>
  );
}

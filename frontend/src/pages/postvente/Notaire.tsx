import { Box, Button, Stack, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { api } from '../../api/client';
import type { DossierNotaire } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import DataTable from '../../components/DataTable';
import { useDialogue } from '../../components/Dialogue';
import FormDialog from '../../components/FormDialog';
import PageHeader from '../../components/PageHeader';
import StatutChip from '../../components/StatutChip';
import { useAction } from '../../components/useAction';
import { aujourdhui, date } from '../../format';

/** Notaire & Actes : dossiers notaire, actes signés (le lot passe "Vendu"). */
export default function Notaire() {
  const { hasRole } = useAuth();
  const navigate = useNavigate();
  const adv = hasRole('ADV');
  const enCours = useQuery({ queryKey: ['notaire-encours'], queryFn: () => api.get<DossierNotaire[]>('/api/postvente/notaire/dossiers') });
  const actes = useQuery({ queryKey: ['notaire-actes'], queryFn: () => api.get<DossierNotaire[]>('/api/postvente/notaire/actes') });
  const dTrans = useDialogue<DossierNotaire>();
  const dActe = useDialogue<DossierNotaire>();
  const transmettre = useAction((v: { id: number; corps: Record<string, unknown> }) => api.post(`/api/postvente/notaire/dossiers/${v.id}/transmettre`, v.corps), 'Dossier transmis au notaire');
  const acte = useAction((v: { id: number; corps: Record<string, unknown> }) => api.post(`/api/postvente/notaire/dossiers/${v.id}/acte`, v.corps), 'Acte signé : lot vendu');

  return (
    <Box>
      <PageHeader titre="Notaire & actes" sousTitre="Ouverture d'un dossier depuis la fiche du dossier de vente (compromis signé)" />
      <Typography variant="subtitle1" fontWeight={600} gutterBottom>Dossiers en cours</Typography>
      <DataTable cle={(d) => d.id} lignes={enCours.data} chargement={enCours.isLoading} vide="Aucun dossier en cours"
        colonnes={[
          { titre: 'Dossier', rendu: (d) => <Button size="small" onClick={() => navigate(`/ventes/reservations/${d.reservationId}`)}>{d.reservationNumero}</Button> },
          { titre: 'Client', rendu: (d) => d.clientNom },
          { titre: 'Lot', rendu: (d) => d.lotCode },
          { titre: 'Notaire', rendu: (d) => d.notaire },
          { titre: 'Transmis le', rendu: (d) => date(d.dateTransmission) },
          { titre: 'Statut', rendu: (d) => <StatutChip statut={d.statut} /> },
          {
            titre: '', rendu: (d) => adv && (
              <Stack direction="row" spacing={1}>
                {d.statut === 'EN_PREPARATION' && <Button size="small" onClick={() => dTrans.ouvrir(d, { dateTransmission: aujourdhui(), observations: d.observations })}>Transmettre</Button>}
                <Button size="small" variant="contained" onClick={() => dActe.ouvrir(d, { dateSignature: aujourdhui() })}>Acte signé</Button>
              </Stack>
            ),
          },
        ]} />
      <Typography variant="subtitle1" fontWeight={600} gutterBottom sx={{ mt: 3 }}>Actes signés</Typography>
      <DataTable cle={(d) => d.id} lignes={actes.data} chargement={actes.isLoading} vide="Aucun acte signé"
        colonnes={[
          { titre: 'Dossier', rendu: (d) => d.reservationNumero },
          { titre: 'Client', rendu: (d) => d.clientNom },
          { titre: 'Lot', rendu: (d) => d.lotCode },
          { titre: 'Notaire', rendu: (d) => d.notaire },
          { titre: 'Acte', rendu: (d) => `n° ${d.numeroActe} du ${date(d.dateSignatureActe)}` },
        ]} />
      <FormDialog ouvert={dTrans.ouvert} titre="Transmission au notaire" initial={dTrans.initial} onFermer={dTrans.fermer}
        champs={[{ nom: 'dateTransmission', label: 'Date', type: 'date', requis: true }, { nom: 'observations', label: 'Observations', type: 'textarea' }]}
        onValider={(v) => transmettre.mutate({ id: dTrans.cible!.id, corps: v }, { onSuccess: dTrans.fermer })} />
      <FormDialog ouvert={dActe.ouvert} titre={`Acte définitif — ${dActe.cible?.reservationNumero ?? ''}`} initial={dActe.initial} onFermer={dActe.fermer}
        texte="Le solde doit être entièrement encaissé (paiement du client ou déblocage du crédit)." libelleValider="Enregistrer l'acte"
        champs={[{ nom: 'numeroActe', label: "N° de l'acte", requis: true }, { nom: 'dateSignature', label: 'Date de signature', type: 'date', requis: true }]}
        onValider={(v) => acte.mutate({ id: dActe.cible!.id, corps: v }, { onSuccess: dActe.fermer })} />
    </Box>
  );
}

import { Box, Button, Card, CardContent, Drawer, Stack, Typography } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { api } from '../../api/client';
import type { Livraison, Ticket } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import DataTable from '../../components/DataTable';
import { useDialogue } from '../../components/Dialogue';
import FormDialog from '../../components/FormDialog';
import PageHeader from '../../components/PageHeader';
import StatutChip from '../../components/StatutChip';
import { useAction } from '../../components/useAction';
import { date, dateHeure, libelle } from '../../format';

const CATEGORIES = ['PLOMBERIE', 'ELECTRICITE', 'MENUISERIE', 'PEINTURE', 'ETANCHEITE', 'CARRELAGE', 'AUTRE'];

/** SAV > Tickets / Interventions / Clôture (12 mois après la livraison). */
export default function Sav() {
  const { hasRole } = useAuth();
  const sav = hasRole('SAV');
  const [ticketId, setTicketId] = useState<number | null>(null);
  const tickets = useQuery({ queryKey: ['tickets'], queryFn: () => api.get<Ticket[]>('/api/postvente/sav/tickets') });
  const livrees = useQuery({ queryKey: ['livrees'], queryFn: () => api.get<Livraison[]>('/api/postvente/livraisons/livrees') });
  const ticket = (tickets.data ?? []).find((t) => t.id === ticketId);
  const dNouveau = useDialogue();
  const dInter = useDialogue();
  const dClot = useDialogue();
  const ouvrir = useAction((v: Record<string, unknown>) => api.post('/api/postvente/sav/tickets', v), 'Ticket ouvert');
  const intervention = useAction((v: Record<string, unknown>) => api.post(`/api/postvente/sav/tickets/${ticketId}/interventions`, v), 'Intervention ajoutée');
  const resoudre = useAction(() => api.post(`/api/postvente/sav/tickets/${ticketId}/resoudre`), 'Ticket résolu');
  const cloturer = useAction((v: Record<string, unknown>) => api.post(`/api/postvente/sav/tickets/${ticketId}/cloturer`, v), 'Ticket clôturé');

  return (
    <Box>
      <PageHeader titre="SAV" sousTitre="Réclamations ouvertes jusqu'à 12 mois après la livraison"
        actions={hasRole('SAV', 'ADV') && <Button variant="contained" startIcon={<AddIcon />} onClick={() => dNouveau.ouvrir(undefined, { priorite: 'NORMALE' })}>Ticket</Button>} />
      <DataTable cle={(t) => t.id} lignes={tickets.data} chargement={tickets.isLoading} vide="Aucun ticket ouvert" onClick={(t) => setTicketId(t.id)}
        colonnes={[
          { titre: 'N°', rendu: (t) => <b>{t.numero}</b> },
          { titre: 'Ouvert le', rendu: (t) => date(t.dateOuverture) },
          { titre: 'Client', rendu: (t) => t.clientNom },
          { titre: 'Lot', rendu: (t) => t.lotCode },
          { titre: 'Catégorie', rendu: (t) => libelle(t.categorie) },
          { titre: 'Priorité', rendu: (t) => <StatutChip statut={t.priorite} /> },
          { titre: 'Statut', rendu: (t) => <StatutChip statut={t.statut} /> },
        ]} />
      <Drawer anchor="right" open={!!ticket} onClose={() => setTicketId(null)}>
        {ticket && (
          <Box sx={{ width: 460, p: 3 }}>
            <Typography variant="h6">{ticket.numero}</Typography>
            <Stack direction="row" spacing={1} sx={{ my: 1 }}><StatutChip statut={ticket.statut} /><StatutChip statut={ticket.priorite} /></Stack>
            <Typography variant="body2">{ticket.clientNom} · {ticket.clientTelephone} · lot {ticket.lotCode}</Typography>
            <Typography variant="body2" sx={{ my: 2 }}>{libelle(ticket.categorie)} : {ticket.description}</Typography>
            {sav && ticket.statut !== 'CLOS' && (
              <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
                <Button size="small" variant="contained" onClick={() => dInter.ouvrir()}>Intervention</Button>
                {ticket.statut !== 'RESOLU' && <Button size="small" onClick={() => resoudre.mutate()}>Résolu</Button>}
                <Button size="small" color="secondary" onClick={() => dClot.ouvrir()}>Clôturer</Button>
              </Stack>
            )}
            <Typography variant="subtitle2">Interventions</Typography>
            {ticket.interventions.map((i) => (
              <Card key={i.id} variant="outlined" sx={{ my: 1 }}><CardContent sx={{ py: 1 }}>
                <Typography variant="caption">{dateHeure(i.dateIntervention)} · {i.intervenant}</Typography>
                <Typography variant="body2">{i.travaux}</Typography>
              </CardContent></Card>
            ))}
          </Box>
        )}
      </Drawer>
      <FormDialog ouvert={dNouveau.ouvert} titre="Nouveau ticket SAV" initial={dNouveau.initial} onFermer={dNouveau.fermer}
        champs={[{ nom: 'livraisonId', label: 'Lot livré', type: 'select', requis: true,
          options: (livrees.data ?? []).map((l) => ({ valeur: l.id, label: `${l.lotCode} · ${l.clientNom} (SAV jusqu'au ${date(l.finGarantieSav)})` })) },
          { nom: 'categorie', label: 'Catégorie', type: 'select', options: CATEGORIES, requis: true },
          { nom: 'priorite', label: 'Priorité', type: 'select', options: ['BASSE', 'NORMALE', 'HAUTE'], requis: true },
          { nom: 'description', label: 'Description', type: 'textarea', requis: true }]}
        onValider={(v) => ouvrir.mutate(v, { onSuccess: dNouveau.fermer })} />
      <FormDialog ouvert={dInter.ouvert} titre="Intervention" onFermer={dInter.fermer}
        champs={[{ nom: 'dateIntervention', label: 'Date et heure', type: 'datetime-local', requis: true },
          { nom: 'intervenant', label: 'Intervenant', requis: true }, { nom: 'travaux', label: 'Travaux réalisés', type: 'textarea', requis: true }]}
        onValider={(v) => intervention.mutate(v, { onSuccess: dInter.fermer })} />
      <FormDialog ouvert={dClot.ouvert} titre="Clôturer le ticket" onFermer={dClot.fermer}
        champs={[{ nom: 'commentaire', label: 'Commentaire', type: 'textarea' }]}
        onValider={(v) => cloturer.mutate(v, { onSuccess: dClot.fermer })} />
    </Box>
  );
}

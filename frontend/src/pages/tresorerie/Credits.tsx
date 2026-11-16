import { Box, Button, Stack } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../api/client';
import type { Credit } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import DataTable from '../../components/DataTable';
import { useDialogue } from '../../components/Dialogue';
import FormDialog from '../../components/FormDialog';
import PageHeader from '../../components/PageHeader';
import StatutChip from '../../components/StatutChip';
import { useAction } from '../../components/useAction';
import { aujourdhui, date, dh } from '../../format';

/** Trésorerie > Crédits bancaires : demande -> accord -> déblocage (= encaissement). */
export default function Credits() {
  const { hasRole } = useAuth();
  const comptable = hasRole('COMPTABLE');
  const credits = useQuery({ queryKey: ['credits'], queryFn: () => api.get<Credit[]>('/api/tresorerie/credits') });
  const dNouveau = useDialogue();
  const dDecision = useDialogue<Credit>();
  const dDeblocage = useDialogue<Credit>();
  const declarer = useAction((v: Record<string, unknown>) => api.post('/api/tresorerie/credits', v), 'Crédit enregistré');
  const decider = useAction((v: { id: number; corps: Record<string, unknown> }) => api.post(`/api/tresorerie/credits/${v.id}/decision`, v.corps), 'Décision de la banque enregistrée');
  const debloquer = useAction((v: { id: number; corps: Record<string, unknown> }) => api.post(`/api/tresorerie/credits/${v.id}/deblocage`, v.corps), 'Déblocage enregistré : encaissement créé');

  return (
    <Box>
      <PageHeader titre="Crédits bancaires" sousTitre="Le déblocage crée automatiquement l'encaissement du dossier"
        actions={comptable && <Button variant="contained" startIcon={<AddIcon />} onClick={() => dNouveau.ouvrir(undefined, { dateDemande: aujourdhui() })}>Nouveau</Button>} />
      <DataTable cle={(c) => c.id} lignes={credits.data} chargement={credits.isLoading} vide="Aucun crédit en cours"
        colonnes={[
          { titre: 'Dossier', rendu: (c) => c.reservationNumero },
          { titre: 'Client', rendu: (c) => c.clientNom },
          { titre: 'Banque', rendu: (c) => c.banque },
          { titre: 'Demandé', rendu: (c) => dh(c.montantDemande), align: 'right' },
          { titre: 'Accordé', rendu: (c) => dh(c.montantAccorde), align: 'right' },
          { titre: 'Depuis le', rendu: (c) => date(c.dateDemande) },
          { titre: 'Statut', rendu: (c) => <StatutChip statut={c.statut} /> },
          {
            titre: '', rendu: (c) => comptable && (
              <Stack direction="row" spacing={1}>
                {c.statut === 'DEMANDE' && <Button size="small" onClick={() => dDecision.ouvrir(c, { accorde: true, montantAccorde: c.montantDemande, date: aujourdhui() })}>Réponse banque</Button>}
                {c.statut === 'ACCORDE' && <Button size="small" variant="contained" onClick={() => dDeblocage.ouvrir(c, { dateDeblocage: aujourdhui() })}>Déblocage</Button>}
              </Stack>
            ),
          },
        ]} />
      <FormDialog ouvert={dNouveau.ouvert} titre="Nouveau crédit bancaire" initial={dNouveau.initial} onFermer={dNouveau.fermer}
        champs={[{ nom: 'reservationId', label: 'Id du dossier', type: 'number', requis: true }, { nom: 'banque', label: 'Banque', requis: true },
          { nom: 'montantDemande', label: 'Montant demandé', type: 'number', requis: true }, { nom: 'dateDemande', label: 'Date', type: 'date', requis: true }]}
        onValider={(v) => declarer.mutate(v, { onSuccess: dNouveau.fermer })} />
      <FormDialog ouvert={dDecision.ouvert} titre="Réponse de la banque" initial={dDecision.initial} onFermer={dDecision.fermer}
        champs={[{ nom: 'accorde', label: 'Crédit accordé', type: 'checkbox' }, { nom: 'montantAccorde', label: 'Montant accordé', type: 'number' },
          { nom: 'date', label: 'Date', type: 'date', requis: true }]}
        onValider={(v) => decider.mutate({ id: dDecision.cible!.id, corps: v }, { onSuccess: dDecision.fermer })} />
      <FormDialog ouvert={dDeblocage.ouvert} titre={`Déblocage de ${dh(dDeblocage.cible?.montantAccorde)}`} initial={dDeblocage.initial} onFermer={dDeblocage.fermer}
        champs={[{ nom: 'dateDeblocage', label: 'Date du virement', type: 'date', requis: true }, { nom: 'reference', label: 'Référence du virement', requis: true }]}
        onValider={(v) => debloquer.mutate({ id: dDeblocage.cible!.id, corps: v }, { onSuccess: dDeblocage.fermer })} />
    </Box>
  );
}

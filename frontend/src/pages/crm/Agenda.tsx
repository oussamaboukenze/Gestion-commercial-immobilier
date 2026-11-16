import { Alert, Box, Button, Grid, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { api, qs } from '../../api/client';
import type { RelanceCrm, RendezVous } from '../../api/types';
import DataTable from '../../components/DataTable';
import { useDialogue } from '../../components/Dialogue';
import FormDialog from '../../components/FormDialog';
import PageHeader from '../../components/PageHeader';
import StatutChip from '../../components/StatutChip';
import { useAction } from '../../components/useAction';
import { date, dateHeure, libelle } from '../../format';

const iso = (d: Date) => d.toISOString().slice(0, 10);

/** CRM > Rendez-vous & visites + "Mes relances du jour". */
export default function Agenda() {
  const du = new Date();
  const au = new Date(Date.now() + 14 * 86_400_000);
  const rdv = useQuery({ queryKey: ['mes-rdv'], queryFn: () => api.get<RendezVous[]>('/api/crm/rendez-vous' + qs({ du: iso(new Date(Date.now() - 7 * 86_400_000)), au: iso(au) })) });
  const enRetard = useQuery({ queryKey: ['cr-retard'], queryFn: () => api.get<RendezVous[]>('/api/crm/rendez-vous/comptes-rendus-en-retard') });
  const relances = useQuery({ queryKey: ['relances-jour'], queryFn: () => api.get<RelanceCrm[]>('/api/crm/relances/du-jour') });
  const dCr = useDialogue<RendezVous>();
  const dFaite = useDialogue<RelanceCrm>();
  const cr = useAction((v: { id: number; texte: unknown }) => api.post(`/api/crm/rendez-vous/${v.id}/compte-rendu`, { compteRendu: v.texte }), 'Compte rendu enregistré');
  const annuler = useAction((id: number) => api.post(`/api/crm/rendez-vous/${id}/annuler`), 'Rendez-vous annulé');
  const faite = useAction((v: { id: number; resultat: unknown }) => api.post(`/api/crm/relances/${v.id}/faite`, { resultat: v.resultat }), 'Relance faite');

  return (
    <Box>
      <PageHeader titre="Agenda & relances" sousTitre={`Du ${du.toLocaleDateString('fr-FR')} au ${au.toLocaleDateString('fr-FR')}`} />
      {(enRetard.data?.length ?? 0) > 0 && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          {enRetard.data!.length} visite(s) sans compte rendu depuis plus de 24 h (règle AHI) : {enRetard.data!.map((r) => r.prospectNom).join(', ')}
        </Alert>
      )}
      <Grid container spacing={3}>
        <Grid size={{ xs: 12, lg: 7 }}>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>Rendez-vous & visites</Typography>
          <DataTable cle={(r) => r.id} lignes={rdv.data} chargement={rdv.isLoading} vide="Aucun rendez-vous"
            colonnes={[
              { titre: 'Date', rendu: (r) => dateHeure(r.dateHeure) },
              { titre: 'Prospect', rendu: (r) => r.prospectNom },
              { titre: 'Type', rendu: (r) => libelle(r.type) },
              { titre: 'Statut', rendu: (r) => <StatutChip statut={r.statut} /> },
              {
                titre: '', rendu: (r) => r.statut === 'PLANIFIE' && (
                  <>
                    {new Date(r.dateHeure) < new Date() && <Button size="small" onClick={() => dCr.ouvrir(r)}>Compte rendu</Button>}
                    <Button size="small" color="error" onClick={() => annuler.mutate(r.id)}>Annuler</Button>
                  </>
                ),
              },
            ]} />
        </Grid>
        <Grid size={{ xs: 12, lg: 5 }}>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>Mes relances du jour</Typography>
          <DataTable cle={(r) => r.id} lignes={relances.data} chargement={relances.isLoading} vide="Rien à relancer aujourd'hui"
            colonnes={[
              { titre: 'Date', rendu: (r) => date(r.dateRelance) },
              { titre: 'Prospect', rendu: (r) => <>{r.prospectNom}<br /><small>{r.telephone}</small></> },
              { titre: 'Objet', rendu: (r) => r.objet },
              { titre: '', rendu: (r) => <Button size="small" onClick={() => dFaite.ouvrir(r)}>Faite</Button> },
            ]} />
        </Grid>
      </Grid>
      <FormDialog ouvert={dCr.ouvert} titre={`Compte rendu — ${dCr.cible?.prospectNom ?? ''}`} onFermer={dCr.fermer}
        champs={[{ nom: 'texte', label: 'Compte rendu', type: 'textarea', requis: true }]}
        onValider={(v) => cr.mutate({ id: dCr.cible!.id, texte: v.texte }, { onSuccess: dCr.fermer })} />
      <FormDialog ouvert={dFaite.ouvert} titre="Relance effectuée" onFermer={dFaite.fermer}
        champs={[{ nom: 'resultat', label: 'Résultat', type: 'textarea' }]}
        onValider={(v) => faite.mutate({ id: dFaite.cible!.id, resultat: v.resultat }, { onSuccess: dFaite.fermer })} />
    </Box>
  );
}

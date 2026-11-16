import { Box, Card, CardContent, Grid, LinearProgress, Stack, Typography } from '@mui/material';
import { useQuery, useQueries } from '@tanstack/react-query';
import { api } from '../api/client';
import type { EtatVentes, Projet, RelanceCrm, Stock } from '../api/types';
import { LOT_STATUTS } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import PageHeader from '../components/PageHeader';
import StatutChip from '../components/StatutChip';
import { dh, libelle } from '../format';

function Kpi({ titre, valeur, sous }: { titre: string; valeur: string | number; sous?: string }) {
  return (
    <Card variant="outlined">
      <CardContent>
        <Typography variant="body2" color="text.secondary">{titre}</Typography>
        <Typography variant="h5" fontWeight={700}>{valeur}</Typography>
        {sous && <Typography variant="caption" color="text.secondary">{sous}</Typography>}
      </CardContent>
    </Card>
  );
}

export default function Dashboard() {
  const { hasRole, nom } = useAuth();
  const voitVentes = hasRole('ADV', 'RESP_COMMERCIAL', 'DAF', 'DIRECTION');
  const estCommercial = hasRole('COMMERCIAL', 'RESP_COMMERCIAL');

  const projets = useQuery({ queryKey: ['projets'], queryFn: () => api.get<Projet[]>('/api/referentiel/projets') });
  const stocks = useQueries({
    queries: (projets.data ?? []).map((p) => ({
      queryKey: ['stock', p.code], queryFn: () => api.get<Stock>(`/api/referentiel/projets/${p.code}/stock`),
    })),
  });
  const ventes = useQueries({
    queries: (voitVentes ? projets.data ?? [] : []).map((p) => ({
      queryKey: ['etat-ventes', p.code], queryFn: () => api.get<EtatVentes>(`/api/ventes/reporting/etat-ventes/${p.code}`),
    })),
  });
  const chiffres = useQuery({
    queryKey: ['mes-chiffres'], enabled: estCommercial,
    queryFn: () => api.get<{ caReserve: number; mois: string }>('/api/ventes/reporting/mes-chiffres'),
  });
  const indicateurs = useQuery({
    queryKey: ['mes-indicateurs'], enabled: estCommercial,
    queryFn: () => api.get<{ visitesEffectuees: number; prospectsConvertis: number; tauxTransformation: number }>('/api/crm/indicateurs/moi'),
  });
  const relances = useQuery({
    queryKey: ['relances-jour'], enabled: estCommercial,
    queryFn: () => api.get<RelanceCrm[]>('/api/crm/relances/du-jour'),
  });

  return (
    <Box>
      <PageHeader titre={`Bonjour ${nom}`} sousTitre="Vue d'ensemble de vos projets" />
      {projets.isLoading && <LinearProgress />}

      {estCommercial && (
        <Grid container spacing={2} sx={{ mb: 3 }}>
          <Grid size={{ xs: 12, md: 3 }}><Kpi titre="CA réservé du mois" valeur={dh(chiffres.data?.caReserve)} sous={chiffres.data?.mois} /></Grid>
          <Grid size={{ xs: 12, md: 3 }}><Kpi titre="Visites effectuées" valeur={indicateurs.data?.visitesEffectuees ?? '—'} /></Grid>
          <Grid size={{ xs: 12, md: 3 }}><Kpi titre="Taux visite → réservation" valeur={`${indicateurs.data?.tauxTransformation ?? 0} %`} /></Grid>
          <Grid size={{ xs: 12, md: 3 }}><Kpi titre="Relances du jour" valeur={relances.data?.length ?? '—'} /></Grid>
        </Grid>
      )}

      <Grid container spacing={2}>
        {(projets.data ?? []).map((p, i) => {
          const stock = stocks[i]?.data;
          const ev = ventes[i]?.data;
          return (
            <Grid key={p.code} size={{ xs: 12, md: 6 }}>
              <Card variant="outlined">
                <CardContent>
                  <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Typography variant="h6">{p.code} · {p.nom}</Typography>
                    <StatutChip statut={p.statut} />
                  </Stack>
                  <Typography variant="body2" color="text.secondary" gutterBottom>{p.ville} — {p.type}</Typography>
                  {stock && (
                    <>
                      <Typography variant="subtitle2" sx={{ mt: 1 }}>Stock : {stock.total} lots</Typography>
                      {LOT_STATUTS.map((s) => {
                        const n = stock.parStatut[s] ?? 0;
                        return (
                          <Box key={s} sx={{ display: 'flex', alignItems: 'center', gap: 1, my: 0.5 }}>
                            <Typography variant="caption" sx={{ width: 120 }}>{libelle(s)}</Typography>
                            <LinearProgress variant="determinate" value={stock.total ? (n * 100) / stock.total : 0}
                              sx={{ flexGrow: 1, height: 8, borderRadius: 4 }} />
                            <Typography variant="caption" sx={{ width: 30, textAlign: 'right' }}>{n}</Typography>
                          </Box>
                        );
                      })}
                    </>
                  )}
                  {ev && (
                    <Typography variant="body2" sx={{ mt: 1 }}>
                      CA réservé actif : <b>{dh(ev.caReserveActif)}</b> · en attente ADV : {ev.parStatut.EN_ATTENTE_VALIDATION_ADV?.nombre ?? 0}
                      {' '}· compromis signés : {ev.parStatut.COMPROMIS_SIGNE?.nombre ?? 0}
                    </Typography>
                  )}
                </CardContent>
              </Card>
            </Grid>
          );
        })}
        {projets.data?.length === 0 && (
          <Typography color="text.secondary" sx={{ m: 2 }}>Aucun projet affecté à votre compte : contactez l'admin SI.</Typography>
        )}
      </Grid>
    </Box>
  );
}

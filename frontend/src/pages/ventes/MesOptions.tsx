import { Box, Button } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { api } from '../../api/client';
import DataTable from '../../components/DataTable';
import PageHeader from '../../components/PageHeader';
import { useAction } from '../../components/useAction';
import { dateHeure } from '../../format';

interface OptionLot { id: number; lotId: number; lotCode: string; projetCode: string; prospectId: number; dateDebut: string; dateExpiration: string }

export default function MesOptions() {
  const navigate = useNavigate();
  const options = useQuery({ queryKey: ['mes-options'], queryFn: () => api.get<OptionLot[]>('/api/ventes/options/mes-options') });
  const lever = useAction((id: number) => api.post(`/api/ventes/options/${id}/lever`), 'Option levée : lot à nouveau disponible');
  const restant = (fin: string) => {
    const h = Math.max(0, Math.round((new Date(fin).getTime() - Date.now()) / 3_600_000));
    return `${h} h`;
  };
  return (
    <Box>
      <PageHeader titre="Mes options" sousTitre="Une option non transformée en réservation sous 48 h libère automatiquement le lot" />
      <DataTable cle={(o) => o.id} lignes={options.data} chargement={options.isLoading} vide="Aucune option active"
        colonnes={[
          { titre: 'Lot', rendu: (o) => <b>{o.lotCode}</b> },
          { titre: 'Projet', rendu: (o) => o.projetCode },
          { titre: 'Prospect', rendu: (o) => `#${o.prospectId}` },
          { titre: 'Posée le', rendu: (o) => dateHeure(o.dateDebut) },
          { titre: 'Expire le', rendu: (o) => `${dateHeure(o.dateExpiration)} (${restant(o.dateExpiration)})` },
          {
            titre: '', rendu: (o) => (
              <>
                <Button size="small" variant="contained" onClick={() => navigate(`/ventes/reservations?nouveau=${o.lotId}`)}>Réserver</Button>
                <Button size="small" color="error" onClick={() => lever.mutate(o.id)}>Lever</Button>
              </>
            ),
          },
        ]} />
    </Box>
  );
}

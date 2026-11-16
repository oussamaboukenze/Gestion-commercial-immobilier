import { Box, Button, Card, CardContent, Stack, TextField, Typography } from '@mui/material';
import FileDownloadIcon from '@mui/icons-material/FileDownload';
import { useState } from 'react';
import { ouvrirDocument, qs } from '../../api/client';
import { useNotifier } from '../../components/Notifier';
import PageHeader from '../../components/PageHeader';

const premierDuMois = () => { const d = new Date(); return new Date(d.getFullYear(), d.getMonth(), 1).toISOString().slice(0, 10); };
const dernierDuMois = () => { const d = new Date(); return new Date(d.getFullYear(), d.getMonth() + 1, 0).toISOString().slice(0, 10); };

/** Reporting > Exports > "Encaissements de la période" vers Sage 100 (fin de chaque mois). */
export default function ExportSage() {
  const notifier = useNotifier();
  const [du, setDu] = useState(premierDuMois());
  const [au, setAu] = useState(dernierDuMois());
  const [enCours, setEnCours] = useState(false);
  const exporter = () => {
    setEnCours(true);
    ouvrirDocument('/api/tresorerie/exports/sage' + qs({ du, au }), `encaissements_${du}_${au}.csv`)
      .then(() => notifier('Export téléchargé'))
      .catch((e: Error) => notifier(e.message, 'error'))
      .finally(() => setEnCours(false));
  };
  return (
    <Box>
      <PageHeader titre="Export comptable Sage 100" sousTitre="La comptabilité générale est tenue dans Sage, pas dans GECIMMO" />
      <Card variant="outlined" sx={{ maxWidth: 560 }}>
        <CardContent>
          <Typography variant="body2" sx={{ mb: 2 }}>
            Fichier CSV (séparateur « ; ») : une écriture par encaissement (débit banque/caisse, crédit client) et
            l'écriture inverse pour les impayés et contre-passations de la période.
          </Typography>
          <Stack direction="row" spacing={2}>
            <TextField size="small" type="date" label="Du" value={du} onChange={(e) => setDu(e.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
            <TextField size="small" type="date" label="Au" value={au} onChange={(e) => setAu(e.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
            <Button variant="contained" startIcon={<FileDownloadIcon />} disabled={enCours} onClick={exporter}>Exporter</Button>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}

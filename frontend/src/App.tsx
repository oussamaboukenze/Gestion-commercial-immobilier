import { CssBaseline, ThemeProvider, createTheme } from '@mui/material';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import Layout from './components/Layout';
import { NotifierProvider } from './components/Notifier';
import Dashboard from './pages/Dashboard';
import ProjetsLots from './pages/referentiel/ProjetsLots';
import GrillesPrix from './pages/referentiel/GrillesPrix';
import Prospects from './pages/crm/Prospects';
import Agenda from './pages/crm/Agenda';
import Reservations from './pages/ventes/Reservations';
import ReservationDetail from './pages/ventes/ReservationDetail';
import MesOptions from './pages/ventes/MesOptions';
import Validations from './pages/ventes/Validations';
import Encaissements from './pages/tresorerie/Encaissements';
import Banque from './pages/tresorerie/Banque';
import Relances from './pages/tresorerie/Relances';
import Credits from './pages/tresorerie/Credits';
import Corrections from './pages/tresorerie/Corrections';
import ExportSage from './pages/tresorerie/ExportSage';
import Notaire from './pages/postvente/Notaire';
import Livraisons from './pages/postvente/Livraisons';
import Sav from './pages/postvente/Sav';
import Utilisateurs from './pages/admin/Utilisateurs';
import JournalPage from './pages/admin/Journal';

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false, staleTime: 10_000 } },
});

const theme = createTheme({
  palette: { primary: { main: '#1f4e79' }, secondary: { main: '#7b5ea7' } },
  shape: { borderRadius: 8 },
  typography: { fontFamily: 'Inter, Roboto, Arial, sans-serif' },
});

export default function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <QueryClientProvider client={queryClient}>
        <NotifierProvider>
          <AuthProvider>
            <BrowserRouter>
              <Routes>
                <Route element={<Layout />}>
                  <Route index element={<Dashboard />} />
                  <Route path="referentiel" element={<ProjetsLots />} />
                  <Route path="referentiel/prix" element={<GrillesPrix />} />
                  <Route path="crm/prospects" element={<Prospects />} />
                  <Route path="crm/agenda" element={<Agenda />} />
                  <Route path="ventes/reservations" element={<Reservations />} />
                  <Route path="ventes/reservations/:id" element={<ReservationDetail />} />
                  <Route path="ventes/options" element={<MesOptions />} />
                  <Route path="ventes/validations" element={<Validations />} />
                  <Route path="tresorerie/encaissements" element={<Encaissements />} />
                  <Route path="tresorerie/banque" element={<Banque />} />
                  <Route path="tresorerie/relances" element={<Relances />} />
                  <Route path="tresorerie/credits" element={<Credits />} />
                  <Route path="tresorerie/corrections" element={<Corrections />} />
                  <Route path="tresorerie/export" element={<ExportSage />} />
                  <Route path="postvente/notaire" element={<Notaire />} />
                  <Route path="postvente/livraisons" element={<Livraisons />} />
                  <Route path="postvente/sav" element={<Sav />} />
                  <Route path="admin/utilisateurs" element={<Utilisateurs />} />
                  <Route path="admin/journal" element={<JournalPage />} />
                  <Route path="*" element={<Navigate to="/" replace />} />
                </Route>
              </Routes>
            </BrowserRouter>
          </AuthProvider>
        </NotifierProvider>
      </QueryClientProvider>
    </ThemeProvider>
  );
}

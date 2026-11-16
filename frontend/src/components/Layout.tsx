import {
  AppBar, Box, Chip, Divider, Drawer, IconButton, List, ListItemButton, ListItemIcon, ListItemText, ListSubheader,
  Toolbar, Tooltip, Typography,
} from '@mui/material';
import DashboardIcon from '@mui/icons-material/Dashboard';
import ApartmentIcon from '@mui/icons-material/Apartment';
import PriceChangeIcon from '@mui/icons-material/PriceChange';
import PeopleIcon from '@mui/icons-material/People';
import EventIcon from '@mui/icons-material/Event';
import AssignmentIcon from '@mui/icons-material/Assignment';
import FactCheckIcon from '@mui/icons-material/FactCheck';
import LockClockIcon from '@mui/icons-material/LockClock';
import PaymentsIcon from '@mui/icons-material/Payments';
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import NotificationsActiveIcon from '@mui/icons-material/NotificationsActive';
import CreditScoreIcon from '@mui/icons-material/CreditScore';
import UndoIcon from '@mui/icons-material/Undo';
import FileDownloadIcon from '@mui/icons-material/FileDownload';
import GavelIcon from '@mui/icons-material/Gavel';
import KeyIcon from '@mui/icons-material/Key';
import BuildIcon from '@mui/icons-material/Build';
import ManageAccountsIcon from '@mui/icons-material/ManageAccounts';
import HistoryIcon from '@mui/icons-material/History';
import LogoutIcon from '@mui/icons-material/Logout';
import type { ReactNode } from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import type { Role } from '../auth/keycloak';
import { libelle } from '../format';

interface Entree { chemin: string; titre: string; icone: ReactNode; roles?: Role[] }
interface Section { titre: string; entrees: Entree[] }

/** Structure des menus GECIMMO (référence interne AHI), filtrée selon le profil. */
export const MENU: Section[] = [
  { titre: '', entrees: [{ chemin: '/', titre: 'Tableau de bord', icone: <DashboardIcon /> }] },
  {
    titre: 'Référentiel', entrees: [
      { chemin: '/referentiel', titre: 'Projets & lots', icone: <ApartmentIcon /> },
      { chemin: '/referentiel/prix', titre: 'Grilles de prix', icone: <PriceChangeIcon />, roles: ['ADMIN_SI', 'DIRECTION'] },
    ],
  },
  {
    titre: 'CRM', entrees: [
      { chemin: '/crm/prospects', titre: 'Prospects', icone: <PeopleIcon />, roles: ['COMMERCIAL', 'RESP_COMMERCIAL', 'ADV', 'DIRECTION'] },
      { chemin: '/crm/agenda', titre: 'Agenda & relances', icone: <EventIcon />, roles: ['COMMERCIAL', 'RESP_COMMERCIAL'] },
    ],
  },
  {
    titre: 'Ventes', entrees: [
      { chemin: '/ventes/reservations', titre: 'Réservations', icone: <AssignmentIcon />, roles: ['COMMERCIAL', 'RESP_COMMERCIAL', 'ADV', 'COMPTABLE', 'DAF', 'DIRECTION'] },
      { chemin: '/ventes/options', titre: 'Mes options', icone: <LockClockIcon />, roles: ['COMMERCIAL', 'RESP_COMMERCIAL'] },
      { chemin: '/ventes/validations', titre: 'Validations', icone: <FactCheckIcon />, roles: ['ADV', 'RESP_COMMERCIAL', 'DAF', 'DIRECTION'] },
    ],
  },
  {
    titre: 'Trésorerie', entrees: [
      { chemin: '/tresorerie/encaissements', titre: 'Encaissements', icone: <PaymentsIcon />, roles: ['COMPTABLE', 'DAF', 'ADV', 'DIRECTION'] },
      { chemin: '/tresorerie/banque', titre: 'Remises & impayés', icone: <AccountBalanceIcon />, roles: ['COMPTABLE', 'DAF'] },
      { chemin: '/tresorerie/relances', titre: 'Relances clients', icone: <NotificationsActiveIcon />, roles: ['COMPTABLE', 'DAF'] },
      { chemin: '/tresorerie/credits', titre: 'Crédits bancaires', icone: <CreditScoreIcon />, roles: ['COMPTABLE', 'DAF', 'ADV'] },
      { chemin: '/tresorerie/corrections', titre: 'Corrections', icone: <UndoIcon />, roles: ['COMPTABLE', 'DAF'] },
      { chemin: '/tresorerie/export', titre: 'Export Sage', icone: <FileDownloadIcon />, roles: ['COMPTABLE', 'DAF'] },
    ],
  },
  {
    titre: 'Notaire, livraison & SAV', entrees: [
      { chemin: '/postvente/notaire', titre: 'Notaire & actes', icone: <GavelIcon />, roles: ['ADV', 'COMPTABLE', 'DAF', 'DIRECTION'] },
      { chemin: '/postvente/livraisons', titre: 'Livraisons', icone: <KeyIcon />, roles: ['ADV', 'SAV', 'DIRECTION'] },
      { chemin: '/postvente/sav', titre: 'SAV', icone: <BuildIcon />, roles: ['SAV', 'ADV', 'DIRECTION'] },
    ],
  },
  {
    titre: 'Administration', entrees: [
      { chemin: '/admin/utilisateurs', titre: 'Utilisateurs', icone: <ManageAccountsIcon />, roles: ['ADMIN_SI'] },
      { chemin: '/admin/journal', titre: "Journal d'activité", icone: <HistoryIcon />, roles: ['ADMIN_SI'] },
    ],
  },
];

const LARGEUR = 250;

export default function Layout() {
  const { hasRole, nom, roles, logout } = useAuth();
  const { pathname } = useLocation();
  const sections = MENU.map((s) => ({ ...s, entrees: s.entrees.filter((e) => !e.roles || hasRole(...e.roles)) }))
    .filter((s) => s.entrees.length > 0);
  const actif = (chemin: string) => (chemin === '/' ? pathname === '/' : pathname === chemin || pathname.startsWith(chemin + '/'));

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar position="fixed" sx={{ zIndex: (t) => t.zIndex.drawer + 1 }}>
        <Toolbar>
          <Typography variant="h6" sx={{ flexGrow: 1, fontWeight: 700 }}>
            GECIMMO <Typography component="span" variant="body2" sx={{ opacity: 0.8 }}>· Atlas Horizon Immobilier</Typography>
          </Typography>
          {roles.map((r) => <Chip key={r} label={libelle(r)} size="small" sx={{ mr: 1, bgcolor: 'rgba(255,255,255,.15)', color: '#fff' }} />)}
          <Typography variant="body2" sx={{ mr: 1 }}>{nom}</Typography>
          <Tooltip title="Se déconnecter"><IconButton color="inherit" onClick={logout}><LogoutIcon /></IconButton></Tooltip>
        </Toolbar>
      </AppBar>
      <Drawer variant="permanent" sx={{ width: LARGEUR, flexShrink: 0, '& .MuiDrawer-paper': { width: LARGEUR, boxSizing: 'border-box' } }}>
        <Toolbar />
        <Box sx={{ overflow: 'auto' }}>
          {sections.map((s, i) => (
            <List key={s.titre || i} dense subheader={s.titre ? <ListSubheader>{s.titre}</ListSubheader> : undefined}>
              {s.entrees.map((e) => (
                <ListItemButton key={e.chemin} component={Link} to={e.chemin} selected={actif(e.chemin)}>
                  <ListItemIcon sx={{ minWidth: 36 }}>{e.icone}</ListItemIcon>
                  <ListItemText primary={e.titre} />
                </ListItemButton>
              ))}
              {i < sections.length - 1 && <Divider />}
            </List>
          ))}
        </Box>
      </Drawer>
      <Box component="main" sx={{ flexGrow: 1, p: 3, bgcolor: '#f6f7f9', minHeight: '100vh' }}>
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  );
}

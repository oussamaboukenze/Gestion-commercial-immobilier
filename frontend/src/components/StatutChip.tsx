import { Chip } from '@mui/material';
import { libelle } from '../format';

type Couleur = 'default' | 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info';

const COULEURS: Record<string, Couleur> = {
  DISPONIBLE: 'success', OPTION: 'warning', RESERVE: 'info', COMPROMIS_SIGNE: 'primary', VENDU: 'secondary',
  LIVRE: 'default',
  EN_ATTENTE_VALIDATION_ADV: 'warning', VALIDEE: 'info', REJETEE: 'error', DESISTEE: 'error',
  NOUVEAU: 'info', EN_COURS: 'warning', CONVERTI: 'success', PERDU: 'default',
  VALIDE: 'success', IMPAYE: 'error', CONTRE_PASSE: 'default',
  A_VENIR: 'default', PARTIELLE: 'warning', PAYEE: 'success', EN_RETARD: 'error',
  EN_ATTENTE_RESP_COMMERCIAL: 'warning', EN_ATTENTE_DAF: 'warning', EN_ATTENTE_DIRECTION: 'warning',
  EN_ATTENTE_VALIDATION_DAF: 'warning', REFUSE: 'error', REFUSEE: 'error', NON_REQUISE: 'default',
  A_ENVOYER: 'warning', ENVOYEE: 'success', ANNULEE: 'default', EN_ATTENTE: 'warning',
  BROUILLON: 'default', SIGNE: 'success', DEMANDE: 'warning', ACCORDE: 'info', DEBLOQUE: 'success',
  EN_PREPARATION: 'warning', TRANSMIS: 'info', ACTE_SIGNE: 'success',
  PLANIFIEE: 'info', PRE_RECEPTION_FAITE: 'warning', LIVREE: 'success',
  OUVERT: 'error', RESOLU: 'info', CLOS: 'default', HAUTE: 'error', NORMALE: 'warning', BASSE: 'default',
  ACTIVE: 'warning', EXPIREE: 'default', TRANSFORMEE: 'success', LEVEE: 'default',
};

export default function StatutChip({ statut }: { statut?: string | null }) {
  if (!statut) return null;
  return <Chip size="small" label={libelle(statut)} color={COULEURS[statut] ?? 'default'} variant="outlined" />;
}

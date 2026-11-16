/** Types TypeScript = DTO JSON du backend GECIMMO. */

export interface Page<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

// ---------- Référentiel ----------
export type LotStatut = 'DISPONIBLE' | 'OPTION' | 'RESERVE' | 'COMPROMIS_SIGNE' | 'VENDU' | 'LIVRE';
export type Typologie = 'STUDIO' | 'F2' | 'F3' | 'F4' | 'DUPLEX' | 'VILLA' | 'COMMERCE' | 'BUREAU' | 'PARKING';
export const TYPOLOGIES: Typologie[] = ['STUDIO', 'F2', 'F3', 'F4', 'DUPLEX', 'VILLA', 'COMMERCE', 'BUREAU', 'PARKING'];
export const LOT_STATUTS: LotStatut[] = ['DISPONIBLE', 'OPTION', 'RESERVE', 'COMPROMIS_SIGNE', 'VENDU', 'LIVRE'];

export interface Projet { id: number; code: string; nom: string; ville: string; type: string; statut: string }
export interface Tranche { id: number; code: string; nom: string; projetCode: string }
export interface Immeuble { id: number; code: string; type: 'IMMEUBLE' | 'ILOT'; nombreEtages: number; trancheId: number }
export interface Lot {
  id: number; code: string; projetCode: string; trancheCode: string; immeubleCode: string; etage: number;
  numero: string; typologie: Typologie; surface: number; prixTTC: number; statut: LotStatut; dateMajStatut: string;
}
export interface Stock { projetCode: string; total: number; parStatut: Record<LotStatut, number> }
export interface DemandePrix {
  id: number; lotId: number; lotCode: string; ancienPrix: number; nouveauPrix: number; motif: string;
  statut: string; demandeur: string; dateDemande: string;
}

// ---------- CRM ----------
export type SourceProspect = 'SITE_WEB' | 'RESEAUX_SOCIAUX' | 'BOUCHE_A_OREILLE' | 'SALON' | 'PANNEAU_CHANTIER'
  | 'APPEL_ENTRANT' | 'PARRAINAGE' | 'AUTRE';
export const SOURCES: SourceProspect[] = ['SITE_WEB', 'RESEAUX_SOCIAUX', 'BOUCHE_A_OREILLE', 'SALON',
  'PANNEAU_CHANTIER', 'APPEL_ENTRANT', 'PARRAINAGE', 'AUTRE'];
export interface Prospect {
  id: number; nom: string; prenom: string; telephone: string; cin: string; email: string; source: SourceProspect;
  budget: number; typologieRecherchee: string; projetCode: string; commercial: string; statut: string;
  motifPerte: string; dateCreation: string;
}
export interface RendezVous {
  id: number; prospectId: number; prospectNom: string; dateHeure: string; type: string; lotId: number;
  statut: string; compteRendu: string; compteRenduHorsDelai: boolean;
}
export interface RelanceCrm {
  id: number; prospectId: number; prospectNom: string; telephone: string; dateRelance: string; objet: string;
  faite: boolean; resultat: string;
}

// ---------- Ventes ----------
export type ReservationStatut = 'EN_ATTENTE_VALIDATION_ADV' | 'VALIDEE' | 'REJETEE' | 'COMPROMIS_SIGNE' | 'DESISTEE';
export interface Reservation {
  id: number; numero: string; lotId: number; lotCode: string; projetCode: string; prospectId: number;
  clientNom: string; clientCin: string; clientTelephone: string; commercial: string; prixTTC: number;
  remisePct: number; prixNet: number; statutRemise: string; avance: number; avanceMinimale: number;
  totalEncaisse: number; avanceConfirmee: boolean; pieceCin: boolean; pieceJustificatifRevenus: boolean;
  pieceRecuAvance: boolean; statut: ReservationStatut; dateCreation: string; dateValidation: string;
  validateurAdv: string; dateLimiteCompromis: string; motifRejet: string;
}
export interface Echeance {
  id: number; ordre: number; libelle: string; pourcentage: number; montant: number; montantPaye: number;
  resteAPayer: number; dateEcheance: string | null; etat: 'A_VENIR' | 'PARTIELLE' | 'PAYEE' | 'EN_RETARD';
}
export interface Compromis {
  id: number; reservationId: number; reservationNumero: string; clientNom: string; lotCode: string; prixNet: number;
  totalEncaisse: number; versementRequisSignature: number; statut: 'BROUILLON' | 'SIGNE'; dateSignature: string;
  echeances: Echeance[];
}
export interface Desistement {
  id: number; reservationId: number; reservationNumero: string; clientNom: string; lotCode: string; motif: string;
  apresCompromis: boolean; totalVerse: number; fraisDossier: number; montantARembourser: number; statut: string;
  saisiPar: string; dateSaisie: string;
}
export interface ChangementLot {
  id: number; reservationId: number; reservationNumero: string; clientNom: string; ancienLotCode: string;
  ancienPrix: number; nouveauLotCode: string; nouveauPrix: number; motif: string; statut: string; saisiPar: string;
}
export interface PieceJointe { id: number; type: string; nomFichier: string; taille: number; dateDepot: string; deposePar: string }
export interface EtatVentes {
  projetCode: string; parStatut: Record<ReservationStatut, { nombre: number; montant: number }>; caReserveActif: number;
}

// ---------- Trésorerie ----------
export type ModePaiement = 'ESPECES' | 'CHEQUE' | 'VIREMENT' | 'VERSEMENT';
export interface Encaissement {
  id: number; numeroRecu: string; reservationId: number; reservationNumero: string; clientNom: string;
  projetCode: string; montant: number; mode: ModePaiement; numeroCheque: string; banque: string; reference: string;
  dateEncaissement: string; statut: 'VALIDE' | 'IMPAYE' | 'CONTRE_PASSE'; origine: string; remiseNumero: string;
}
export interface Remise { id: number; numero: string; banque: string; dateRemise: string; montantTotal: number; nombreCheques: number }
export interface Impaye { id: number; numeroRecu: string; clientNom: string; reservationNumero: string; montant: number; numeroCheque: string; motif: string; dateRejet: string }
export interface Correction { id: number; encaissementId: number; numeroRecu: string; clientNom: string; montant: number; motif: string; statut: string; demandeur: string; dateDemande: string }
export interface RelanceClient {
  id: number; reservationId: number; reservationNumero: string; clientNom: string; projetCode: string; objet: string;
  montantDu: number; dateEcheance: string; niveau: string; statut: string; dateCreation: string;
}
export interface Credit {
  id: number; reservationId: number; reservationNumero: string; clientNom: string; banque: string;
  montantDemande: number; montantAccorde: number; statut: string; dateDemande: string;
}

// ---------- Après-vente ----------
export interface DossierNotaire {
  id: number; reservationId: number; reservationNumero: string; lotCode: string; projetCode: string; clientNom: string;
  notaire: string; observations: string; statut: 'EN_PREPARATION' | 'TRANSMIS' | 'ACTE_SIGNE';
  dateTransmission: string; dateSignatureActe: string; numeroActe: string; resteAPayer: number | null;
}
export interface Livraison {
  id: number; reservationId: number; reservationNumero: string; lotCode: string; projetCode: string; clientNom: string;
  clientTelephone: string; datePrevue: string; statut: 'PLANIFIEE' | 'PRE_RECEPTION_FAITE' | 'LIVREE' | 'ANNULEE';
  reserves: string; reservesLevees: boolean; dateLivraison: string; nombreCles: number; finGarantieSav: string;
}
export interface Ticket {
  id: number; numero: string; livraisonId: number; lotCode: string; projetCode: string; clientNom: string;
  clientTelephone: string; categorie: string; priorite: string; description: string; statut: string;
  dateOuverture: string; interventions: { id: number; dateIntervention: string; intervenant: string; travaux: string }[];
}

// ---------- Administration ----------
export interface UtilisateurAdmin {
  id: string; username: string; prenom: string; nom: string; email: string; actif: boolean; profil: string | null;
  projets: string[];
}
export interface Journal { id: number; date: string; evenement: string; details: string }

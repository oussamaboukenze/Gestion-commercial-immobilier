package ma.ahi.gecimmo.vente.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Règles de vente AHI regroupées dans une classe sans dépendance Spring :
 * facile à lire, à tester et à faire évoluer.
 */
public final class RegleVente {

    public static final BigDecimal CENT = BigDecimal.valueOf(100);
    /** Avance de réservation : 5 % du prix TTC minimum. */
    public static final BigDecimal TAUX_AVANCE_MIN = new BigDecimal("5");
    /** Versement cumulé à la signature du compromis : 20 % du prix. */
    public static final BigDecimal TAUX_SIGNATURE_COMPROMIS = new BigDecimal("20");
    /** Frais de dossier retenus sur un désistement avant compromis : 10 %. */
    public static final BigDecimal TAUX_FRAIS_DESISTEMENT = new BigDecimal("10");
    /** Remise : commercial jusqu'à 2 %, responsable commercial jusqu'à 5 %, direction au-delà. */
    public static final BigDecimal REMISE_MAX_COMMERCIAL = new BigDecimal("2");
    public static final BigDecimal REMISE_MAX_RESPONSABLE = new BigDecimal("5");
    /** Durée de validité d'une option (heures) et d'une réservation (jours). */
    public static final int DUREE_OPTION_HEURES = 48;
    public static final int VALIDITE_RESERVATION_JOURS = 30;

    /** Echéancier standard : 20 % compromis / 30 % gros oeuvre / 50 % à l'acte. */
    public static final List<EcheanceModele> ECHEANCIER_STANDARD = List.of(
            new EcheanceModele("Signature du compromis", new BigDecimal("20")),
            new EcheanceModele("Achèvement du gros oeuvre", new BigDecimal("30")),
            new EcheanceModele("Signature de l'acte (ou déblocage du crédit)", new BigDecimal("50")));

    public record EcheanceModele(String libelle, BigDecimal pourcentage) {
    }

    private RegleVente() {
    }

    public static BigDecimal arrondir(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal pourcentage(BigDecimal base, BigDecimal taux) {
        return arrondir(base.multiply(taux).divide(CENT, 6, RoundingMode.HALF_UP));
    }

    public static BigDecimal prixNet(BigDecimal prixTTC, BigDecimal remisePct) {
        BigDecimal remise = remisePct == null ? BigDecimal.ZERO : remisePct;
        return arrondir(prixTTC.subtract(pourcentage(prixTTC, remise)));
    }

    public static BigDecimal avanceMinimale(BigDecimal prixNet) {
        return pourcentage(prixNet, TAUX_AVANCE_MIN);
    }

    public static BigDecimal versementMinimalCompromis(BigDecimal prixNet) {
        return pourcentage(prixNet, TAUX_SIGNATURE_COMPROMIS);
    }

    /** Qui doit valider la remise ? (matrice de validation AHI) */
    public static StatutRemise statutRemiseInitial(BigDecimal remisePct) {
        if (remisePct == null || remisePct.signum() == 0) {
            return StatutRemise.NON_REQUISE;
        }
        if (remisePct.signum() < 0 || remisePct.compareTo(CENT) >= 0) {
            throw new IllegalArgumentException("Remise invalide : " + remisePct + " %");
        }
        if (remisePct.compareTo(REMISE_MAX_COMMERCIAL) <= 0) {
            return StatutRemise.NON_REQUISE;
        }
        if (remisePct.compareTo(REMISE_MAX_RESPONSABLE) <= 0) {
            return StatutRemise.EN_ATTENTE_RESP_COMMERCIAL;
        }
        return StatutRemise.EN_ATTENTE_DIRECTION;
    }

    /** Montants de l'échéancier standard ; la dernière échéance absorbe l'arrondi. */
    public static List<BigDecimal> montantsEcheancier(BigDecimal prixNet) {
        List<BigDecimal> montants = new ArrayList<>();
        BigDecimal cumul = BigDecimal.ZERO;
        for (int i = 0; i < ECHEANCIER_STANDARD.size(); i++) {
            BigDecimal montant = i == ECHEANCIER_STANDARD.size() - 1
                    ? arrondir(prixNet.subtract(cumul))
                    : pourcentage(prixNet, ECHEANCIER_STANDARD.get(i).pourcentage());
            montants.add(montant);
            cumul = cumul.add(montant);
        }
        return montants;
    }

    /**
     * Répartit le total encaissé sur les échéances dans l'ordre (la plus ancienne d'abord).
     * Ainsi un paiement est toujours rattaché à la bonne échéance.
     */
    public static List<BigDecimal> repartir(BigDecimal totalEncaisse, List<BigDecimal> montantsEcheances) {
        List<BigDecimal> payes = new ArrayList<>();
        BigDecimal reste = totalEncaisse.max(BigDecimal.ZERO);
        for (BigDecimal du : montantsEcheances) {
            BigDecimal paye = reste.min(du);
            payes.add(arrondir(paye));
            reste = reste.subtract(paye);
        }
        return payes;
    }

    /** Désistement avant compromis : remboursement des versements moins 10 % de frais de dossier. */
    public static BigDecimal fraisDossierDesistement(BigDecimal totalVerse) {
        return pourcentage(totalVerse, TAUX_FRAIS_DESISTEMENT);
    }
}

package ma.ahi.gecimmo.tresorerie.client;

import java.math.BigDecimal;

/** Dossier de vente renvoyé par vente-service (champs utiles à la trésorerie). */
public record ReservationDto(Long id, String numero, String lotCode, String projetCode, String clientNom,
                             String clientCin, BigDecimal prixNet, BigDecimal avance, BigDecimal totalEncaisse,
                             String statut) {

    public boolean estActive() {
        return "EN_ATTENTE_VALIDATION_ADV".equals(statut) || "VALIDEE".equals(statut)
                || "COMPROMIS_SIGNE".equals(statut);
    }

    public BigDecimal resteAPayer() {
        return prixNet.subtract(totalEncaisse);
    }
}

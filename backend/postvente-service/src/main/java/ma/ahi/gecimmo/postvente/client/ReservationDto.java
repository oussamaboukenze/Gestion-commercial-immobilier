package ma.ahi.gecimmo.postvente.client;

import java.math.BigDecimal;

/** Dossier de vente (vente-service). */
public record ReservationDto(Long id, String numero, Long lotId, String lotCode, String projetCode, String clientNom,
                             String clientCin, String clientTelephone, BigDecimal prixNet, BigDecimal totalEncaisse,
                             String statut) {

    public BigDecimal resteAPayer() {
        return prixNet.subtract(totalEncaisse).max(BigDecimal.ZERO);
    }

    public boolean soldeNul() {
        return resteAPayer().signum() == 0;
    }
}

package ma.ahi.gecimmo.tresorerie.messaging;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Events {

    private Events() {
    }

    /** -> vente-service : type = ENREGISTRE ou ANNULE (impayé / contre-passation). */
    public record EncaissementEvent(Long encaissementId, Long reservationId, Long echeanceId, BigDecimal montant,
                                    String type) {
    }

    /** <- vente-service : échéance non payée à la date prévue. */
    public record EcheanceEnRetardEvent(Long echeanceId, Long reservationId, String reservationNumero,
                                        String clientNom, String projetCode, String libelle,
                                        BigDecimal resteAPayer, LocalDate dateEcheance, long joursRetard) {
    }
}

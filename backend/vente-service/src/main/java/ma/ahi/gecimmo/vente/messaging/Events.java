package ma.ahi.gecimmo.vente.messaging;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import ma.ahi.gecimmo.vente.domain.LotStatut;

/** Contrats des messages RabbitMQ (JSON) publiés / reçus par vente-service. */
public final class Events {

    private Events() {
    }

    /** -> referentiel-service */
    public record LotStatutChangeEvent(Long lotId, LotStatut nouveauStatut, List<LotStatut> statutsAttendus,
                                       String motif) {
    }

    /** -> crm-service */
    public record ReservationValideeEvent(Long reservationId, Long prospectId, Long lotId, String lotCode,
                                          String projetCode, String commercial, BigDecimal prixNet) {
    }

    /** -> tresorerie-service (relances J+7 / J+15 / mise en demeure J+30) */
    public record EcheanceEnRetardEvent(Long echeanceId, Long reservationId, String reservationNumero,
                                        String clientNom, String projetCode, String libelle,
                                        BigDecimal resteAPayer, LocalDate dateEcheance, long joursRetard) {
    }

    /** <- tresorerie-service */
    public record EncaissementEvent(Long encaissementId, Long reservationId, Long echeanceId, BigDecimal montant,
                                    String type) {
    }
}

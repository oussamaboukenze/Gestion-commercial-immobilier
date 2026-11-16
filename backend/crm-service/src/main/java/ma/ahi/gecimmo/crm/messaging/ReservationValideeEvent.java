package ma.ahi.gecimmo.crm.messaging;

import java.math.BigDecimal;

/** Publié par vente-service quand l'ADV valide une réservation. */
public record ReservationValideeEvent(Long reservationId, Long prospectId, Long lotId, String lotCode,
                                      String projetCode, String commercial, BigDecimal prixNet) {
}

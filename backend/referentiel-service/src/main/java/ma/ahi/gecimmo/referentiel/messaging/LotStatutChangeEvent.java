package ma.ahi.gecimmo.referentiel.messaging;

import java.util.List;

import ma.ahi.gecimmo.referentiel.domain.LotStatut;

/** Evénement publié par vente-service (option expirée, compromis signé, désistement...). */
public record LotStatutChangeEvent(Long lotId, LotStatut nouveauStatut, List<LotStatut> statutsAttendus,
                                   String motif) {
}

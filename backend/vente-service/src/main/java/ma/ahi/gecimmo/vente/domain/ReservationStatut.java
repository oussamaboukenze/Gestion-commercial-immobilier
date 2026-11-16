package ma.ahi.gecimmo.vente.domain;

import java.util.EnumSet;
import java.util.Set;

public enum ReservationStatut {
    EN_ATTENTE_VALIDATION_ADV, VALIDEE, REJETEE, COMPROMIS_SIGNE, DESISTEE;

    /** Statuts où la réservation "occupe" le lot. */
    public static final Set<ReservationStatut> ACTIFS = EnumSet.of(EN_ATTENTE_VALIDATION_ADV, VALIDEE, COMPROMIS_SIGNE);
}

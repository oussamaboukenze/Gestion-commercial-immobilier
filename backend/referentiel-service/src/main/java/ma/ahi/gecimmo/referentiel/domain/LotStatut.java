package ma.ahi.gecimmo.referentiel.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Statuts d'un lot (un seul statut à tout moment).
 * Un désistement remet le lot à DISPONIBLE : "Désisté" est un statut de la vente, pas du lot.
 */
public enum LotStatut {
    DISPONIBLE, OPTION, RESERVE, COMPROMIS_SIGNE, VENDU, LIVRE;

    /** Transitions autorisées : protège le stock contre les incohérences. */
    public Set<LotStatut> transitionsPossibles() {
        return switch (this) {
            case DISPONIBLE -> EnumSet.of(OPTION, RESERVE);
            case OPTION -> EnumSet.of(DISPONIBLE, RESERVE);
            case RESERVE -> EnumSet.of(COMPROMIS_SIGNE, DISPONIBLE);
            case COMPROMIS_SIGNE -> EnumSet.of(VENDU, DISPONIBLE);
            case VENDU -> EnumSet.of(LIVRE);
            case LIVRE -> EnumSet.noneOf(LotStatut.class);
        };
    }

    public boolean peutPasserA(LotStatut cible) {
        return transitionsPossibles().contains(cible);
    }
}

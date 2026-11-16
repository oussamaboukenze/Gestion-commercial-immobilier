package ma.ahi.gecimmo.vente.domain;

public enum StatutRemise {
    /** Pas de remise ou remise <= 2 % (pouvoir du commercial). */
    NON_REQUISE,
    /** Remise entre 2 % et 5 % : validation du responsable commercial. */
    EN_ATTENTE_RESP_COMMERCIAL,
    /** Remise > 5 % : validation de la direction. */
    EN_ATTENTE_DIRECTION,
    VALIDEE,
    REFUSEE;

    public boolean estAcquise() {
        return this == NON_REQUISE || this == VALIDEE;
    }
}

package ma.ahi.gecimmo.vente.client;

import java.math.BigDecimal;

import ma.ahi.gecimmo.vente.domain.LotStatut;

/** Lot tel que renvoyé par referentiel-service (champs utiles à la vente). */
public record LotDto(Long id, String code, String projetCode, String typologie, BigDecimal surface,
                     BigDecimal prixTTC, LotStatut statut) {
}

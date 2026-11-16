package ma.ahi.gecimmo.tresorerie.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paramètres de la politique interne AHI (application.yml, préfixe gecimmo.tresorerie).
 *
 * @param plafondEspeces montant maximum accepté en espèces, fixé par la politique interne.
 *                       0 = non paramétré (aucun contrôle) : à renseigner avec le DAF.
 * @param sage           comptes et journal utilisés pour l'export vers Sage 100 (à valider par la comptabilité).
 */
@ConfigurationProperties(prefix = "gecimmo.tresorerie")
public record TresorerieProperties(BigDecimal plafondEspeces, Sage sage) {

    public record Sage(String journalBanque, String journalCaisse, String compteBanque, String compteCaisse,
                       String compteClient) {
    }

    public boolean plafondEspecesActif() {
        return plafondEspeces != null && plafondEspeces.signum() > 0;
    }
}

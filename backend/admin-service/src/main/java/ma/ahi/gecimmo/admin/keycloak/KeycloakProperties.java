package ma.ahi.gecimmo.admin.keycloak;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Connexion à l'API d'administration Keycloak (client "gecimmo-admin" avec compte de service). */
@ConfigurationProperties(prefix = "gecimmo.keycloak")
public record KeycloakProperties(String serverUrl, String realm, String clientId, String clientSecret) {
}

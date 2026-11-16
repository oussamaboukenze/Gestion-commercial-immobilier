package ma.ahi.gecimmo.admin.keycloak;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import ma.ahi.gecimmo.admin.exception.BusinessException;

/**
 * Appels REST à l'API d'administration de Keycloak.
 * Le service s'authentifie avec son propre compte (client credentials) : aucun mot de passe utilisateur ici.
 */
@Component
public class KeycloakAdminClient {

    public record UserRep(String id, String username, String firstName, String lastName, String email,
                          Boolean enabled, Long createdTimestamp) {
    }

    public record RoleRep(String id, String name) {
    }

    public record GroupRep(String id, String name, String path) {
    }

    private record TokenResponse(String access_token, long expires_in) {
    }

    private static final ParameterizedTypeReference<List<UserRep>> USERS = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<RoleRep>> ROLES = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<GroupRep>> GROUPS = new ParameterizedTypeReference<>() { };

    private final KeycloakProperties props;
    private final RestClient rest;
    private String token;
    private Instant tokenExpiration = Instant.EPOCH;

    public KeycloakAdminClient(KeycloakProperties props, RestClient.Builder builder) {
        this.props = props;
        this.rest = builder.baseUrl(props.serverUrl()).build();
    }

    private synchronized String token() {
        if (token == null || Instant.now().isAfter(tokenExpiration.minusSeconds(30))) {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", props.clientId());
            form.add("client_secret", props.clientSecret());
            TokenResponse t = rest.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", props.realm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            if (t == null) {
                throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "Keycloak ne répond pas.");
            }
            token = t.access_token();
            tokenExpiration = Instant.now().plusSeconds(t.expires_in());
        }
        return token;
    }

    private String admin(String path) {
        return "/admin/realms/" + props.realm() + path;
    }

    public List<UserRep> listerUtilisateurs(String recherche) {
        return rest.get()
                .uri(b -> b.path(admin("/users")).queryParam("max", 500).queryParam("briefRepresentation", true)
                        .queryParamIfPresent("search", java.util.Optional.ofNullable(recherche)).build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                .retrieve().body(USERS);
    }

    public UserRep utilisateur(String id) {
        return rest.get().uri(admin("/users/{id}"), id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                .retrieve().body(UserRep.class);
    }

    /** Crée l'utilisateur et renvoie son id (lu dans l'en-tête Location). */
    public String creerUtilisateur(String username, String prenom, String nom, String email,
                                   String motDePasseTemporaire) {
        Map<String, Object> body = Map.of(
                "username", username, "firstName", prenom, "lastName", nom, "email", email,
                "enabled", true, "emailVerified", true,
                "credentials", List.of(Map.of("type", "password", "value", motDePasseTemporaire,
                        "temporary", true)));
        try {
            ResponseEntity<Void> resp = rest.post().uri(admin("/users"))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                    .contentType(MediaType.APPLICATION_JSON).body(body)
                    .retrieve().toBodilessEntity();
            URI location = resp.getHeaders().getLocation();
            if (location == null) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "Réponse Keycloak inattendue.");
            }
            String path = location.getPath();
            return path.substring(path.lastIndexOf('/') + 1);
        } catch (HttpClientErrorException.Conflict e) {
            throw BusinessException.conflict("L'identifiant ou l'email " + username + " existe déjà.");
        }
    }

    public void activer(String id, boolean actif) {
        rest.put().uri(admin("/users/{id}"), id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("enabled", actif))
                .retrieve().toBodilessEntity();
    }

    public void reinitialiserMotDePasse(String id, String motDePasseTemporaire) {
        rest.put().uri(admin("/users/{id}/reset-password"), id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("type", "password", "value", motDePasseTemporaire, "temporary", true))
                .retrieve().toBodilessEntity();
    }

    public List<RoleRep> rolesUtilisateur(String id) {
        return rest.get().uri(admin("/users/{id}/role-mappings/realm"), id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                .retrieve().body(ROLES);
    }

    public RoleRep role(String nom) {
        return rest.get().uri(admin("/roles/{nom}"), nom)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                .retrieve().body(RoleRep.class);
    }

    public void ajouterRoles(String id, List<RoleRep> roles) {
        rest.post().uri(admin("/users/{id}/role-mappings/realm"), id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                .contentType(MediaType.APPLICATION_JSON).body(roles)
                .retrieve().toBodilessEntity();
    }

    public void retirerRoles(String id, List<RoleRep> roles) {
        rest.method(org.springframework.http.HttpMethod.DELETE).uri(admin("/users/{id}/role-mappings/realm"), id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                .contentType(MediaType.APPLICATION_JSON).body(roles)
                .retrieve().toBodilessEntity();
    }

    public List<GroupRep> groupes() {
        return rest.get().uri(b -> b.path(admin("/groups")).queryParam("max", 200).build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                .retrieve().body(GROUPS);
    }

    public List<GroupRep> groupesUtilisateur(String id) {
        return rest.get().uri(admin("/users/{id}/groups"), id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                .retrieve().body(GROUPS);
    }

    public void ajouterGroupe(String userId, String groupId) {
        rest.put().uri(admin("/users/{id}/groups/{g}"), userId, groupId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                .retrieve().toBodilessEntity();
    }

    public void retirerGroupe(String userId, String groupId) {
        rest.delete().uri(admin("/users/{id}/groups/{g}"), userId, groupId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                .retrieve().toBodilessEntity();
    }

    public void creerGroupe(String nom) {
        rest.post().uri(admin("/groups"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("name", nom))
                .retrieve().toBodilessEntity();
    }
}

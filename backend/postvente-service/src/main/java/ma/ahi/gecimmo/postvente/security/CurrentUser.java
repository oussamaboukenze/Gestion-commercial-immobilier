package ma.ahi.gecimmo.postvente.security;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import ma.ahi.gecimmo.postvente.exception.BusinessException;

/**
 * Utilisateur connecté (lu depuis le JWT Keycloak).
 * Projets autorisés : groupes Keycloak "projet-RNA", "projet-JBK"...
 * Direction et Admin SI voient tous les projets.
 */
@Component
public class CurrentUser {

    private static final String PREFIXE_GROUPE_PROJET = "projet-";

    public String username() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : auth.getName();
    }

    public Set<String> roles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Set.of();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replaceFirst("^ROLE_", ""))
                .collect(Collectors.toSet());
    }

    public boolean hasRole(String role) {
        return roles().contains(role);
    }

    public boolean estCommercialSimple() {
        return hasRole("COMMERCIAL") && !hasRole("RESP_COMMERCIAL") && !hasRole("DIRECTION");
    }

    public boolean voitTousLesProjets() {
        return hasRole("DIRECTION") || hasRole("ADMIN_SI");
    }

    public Set<String> projetsAutorises() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            return Set.of();
        }
        List<String> groups = jwt.getClaimAsStringList("groups");
        if (groups == null) {
            return Set.of();
        }
        return groups.stream()
                .map(g -> g.startsWith("/") ? g.substring(1) : g)
                .filter(g -> g.startsWith(PREFIXE_GROUPE_PROJET))
                .map(g -> g.substring(PREFIXE_GROUPE_PROJET.length()).toUpperCase())
                .collect(Collectors.toSet());
    }

    public boolean peutVoirProjet(String codeProjet) {
        return voitTousLesProjets() || projetsAutorises().contains(codeProjet);
    }

    /** Règle AHI : ne jamais exposer les données d'un projet non affecté à l'utilisateur. */
    public void verifierAccesProjet(String codeProjet) {
        if (!peutVoirProjet(codeProjet)) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Vous n'êtes pas affecté au projet " + codeProjet + ". Contactez l'admin SI.");
        }
    }
}

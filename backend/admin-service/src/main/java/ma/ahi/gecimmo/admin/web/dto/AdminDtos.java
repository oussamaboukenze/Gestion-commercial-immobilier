package ma.ahi.gecimmo.admin.web.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import ma.ahi.gecimmo.admin.domain.JournalActivite;

public final class AdminDtos {

    private AdminDtos() {
    }

    /** Profils GECIMMO (= rôles Keycloak). */
    public enum Profil { COMMERCIAL, ADV, COMPTABLE, ADMIN_SI, RESP_COMMERCIAL, DAF, SAV, DIRECTION }

    public record UtilisateurRequest(
            @NotBlank @Pattern(regexp = "^[a-z0-9._-]{3,30}$", message = "minuscules, chiffres, . _ - (3 à 30)") String username,
            @NotBlank String prenom,
            @NotBlank String nom,
            @NotBlank @Email String email,
            @NotNull Profil profil,
            List<String> projets) {
    }

    public record DroitsRequest(@NotNull Profil profil, List<String> projets) {
    }

    public record UtilisateurResponse(String id, String username, String prenom, String nom, String email,
                                      boolean actif, Profil profil, List<String> projets) {
    }

    /** Le mot de passe temporaire est affiché une seule fois à l'admin SI (changement imposé à la 1re connexion). */
    public record MotDePasseResponse(String utilisateurId, String motDePasseTemporaire) {
    }

    public record CreationResponse(UtilisateurResponse utilisateur, String motDePasseTemporaire) {
    }

    public record JournalResponse(Long id, LocalDateTime date, String evenement, String details) {
        public static JournalResponse of(JournalActivite j) {
            return new JournalResponse(j.getId(), j.getDate(), j.getEvenement(), j.getDetails());
        }
    }
}

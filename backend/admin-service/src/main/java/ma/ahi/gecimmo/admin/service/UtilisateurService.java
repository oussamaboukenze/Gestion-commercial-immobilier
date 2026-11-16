package ma.ahi.gecimmo.admin.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ma.ahi.gecimmo.admin.exception.BusinessException;
import ma.ahi.gecimmo.admin.keycloak.KeycloakAdminClient;
import ma.ahi.gecimmo.admin.keycloak.KeycloakAdminClient.GroupRep;
import ma.ahi.gecimmo.admin.keycloak.KeycloakAdminClient.RoleRep;
import ma.ahi.gecimmo.admin.keycloak.KeycloakAdminClient.UserRep;
import ma.ahi.gecimmo.admin.security.CurrentUser;
import ma.ahi.gecimmo.admin.web.dto.AdminDtos.CreationResponse;
import ma.ahi.gecimmo.admin.web.dto.AdminDtos.DroitsRequest;
import ma.ahi.gecimmo.admin.web.dto.AdminDtos.MotDePasseResponse;
import ma.ahi.gecimmo.admin.web.dto.AdminDtos.Profil;
import ma.ahi.gecimmo.admin.web.dto.AdminDtos.UtilisateurRequest;
import ma.ahi.gecimmo.admin.web.dto.AdminDtos.UtilisateurResponse;

/**
 * Administration > Utilisateurs / Profils & droits.
 * Principe du moindre privilège : un profil + les projets autorisés. On ne supprime jamais un compte
 * (historique) : on le désactive.
 */
@Service
public class UtilisateurService {

    private static final String PREFIXE_PROJET = "projet-";
    private static final Set<String> PROFILS = Arrays.stream(Profil.values()).map(Enum::name).collect(Collectors.toSet());
    private static final SecureRandom RANDOM = new SecureRandom();

    private final KeycloakAdminClient keycloak;
    private final JournalService journalService;
    private final CurrentUser currentUser;

    public UtilisateurService(KeycloakAdminClient keycloak, JournalService journalService, CurrentUser currentUser) {
        this.keycloak = keycloak;
        this.journalService = journalService;
        this.currentUser = currentUser;
    }

    public List<UtilisateurResponse> lister(String recherche) {
        List<UtilisateurResponse> liste = new ArrayList<>();
        for (UserRep u : keycloak.listerUtilisateurs(recherche)) {
            if (u.username().startsWith("service-account-")) {
                continue;
            }
            liste.add(versReponse(u));
        }
        return liste;
    }

    public UtilisateurResponse get(String id) {
        return versReponse(keycloak.utilisateur(id));
    }

    public CreationResponse creer(UtilisateurRequest req) {
        String motDePasse = motDePasseTemporaire();
        String id = keycloak.creerUtilisateur(req.username(), req.prenom(), req.nom(), req.email(), motDePasse);
        appliquerDroits(id, req.profil(), req.projets());
        tracer("admin.utilisateur.cree", req.username() + " (" + req.profil() + ", projets " + req.projets() + ")");
        return new CreationResponse(get(id), motDePasse);
    }

    public UtilisateurResponse modifierDroits(String id, DroitsRequest req) {
        appliquerDroits(id, req.profil(), req.projets());
        UtilisateurResponse u = get(id);
        tracer("admin.utilisateur.droits", u.username() + " -> " + req.profil() + ", projets " + req.projets());
        return u;
    }

    /** Départ d'un collaborateur : désactiver (jamais supprimer). Penser à réaffecter ses prospects (CRM). */
    public UtilisateurResponse activer(String id, boolean actif) {
        keycloak.activer(id, actif);
        UtilisateurResponse u = get(id);
        tracer(actif ? "admin.utilisateur.active" : "admin.utilisateur.desactive", u.username());
        return u;
    }

    public MotDePasseResponse reinitialiserMotDePasse(String id) {
        String motDePasse = motDePasseTemporaire();
        keycloak.reinitialiserMotDePasse(id, motDePasse);
        tracer("admin.utilisateur.mot-de-passe", get(id).username());
        return new MotDePasseResponse(id, motDePasse);
    }

    public List<String> projetsDisponibles() {
        return keycloak.groupes().stream().map(GroupRep::name)
                .filter(n -> n.startsWith(PREFIXE_PROJET))
                .map(n -> n.substring(PREFIXE_PROJET.length()))
                .sorted().toList();
    }

    // ---------- interne ----------

    private void appliquerDroits(String userId, Profil profil, List<String> projets) {
        // 1 seul profil GECIMMO par utilisateur
        List<RoleRep> actuels = keycloak.rolesUtilisateur(userId).stream()
                .filter(r -> PROFILS.contains(r.name())).toList();
        List<RoleRep> aRetirer = actuels.stream().filter(r -> !r.name().equals(profil.name())).toList();
        if (!aRetirer.isEmpty()) {
            keycloak.retirerRoles(userId, aRetirer);
        }
        if (actuels.stream().noneMatch(r -> r.name().equals(profil.name()))) {
            keycloak.ajouterRoles(userId, List.of(keycloak.role(profil.name())));
        }

        // Projets autorisés = groupes "projet-XXX"
        Set<String> voulus = projets == null ? Set.of()
                : projets.stream().map(p -> PREFIXE_PROJET + p.trim().toUpperCase()).collect(Collectors.toSet());
        Map<String, String> groupesParNom = new HashMap<>();
        for (GroupRep g : keycloak.groupes()) {
            groupesParNom.put(g.name(), g.id());
        }
        for (String nom : voulus) {
            if (!groupesParNom.containsKey(nom)) {
                keycloak.creerGroupe(nom);
                keycloak.groupes().stream().filter(g -> g.name().equals(nom)).findFirst()
                        .ifPresent(g -> groupesParNom.put(g.name(), g.id()));
            }
        }
        List<GroupRep> actuelsGroupes = keycloak.groupesUtilisateur(userId);
        for (GroupRep g : actuelsGroupes) {
            if (g.name().startsWith(PREFIXE_PROJET) && !voulus.contains(g.name())) {
                keycloak.retirerGroupe(userId, g.id());
            }
        }
        Set<String> dejaMembre = actuelsGroupes.stream().map(GroupRep::name).collect(Collectors.toSet());
        for (String nom : voulus) {
            String gid = groupesParNom.get(nom);
            if (gid == null) {
                throw new BusinessException("Impossible de créer le groupe " + nom + " dans Keycloak.");
            }
            if (!dejaMembre.contains(nom)) {
                keycloak.ajouterGroupe(userId, gid);
            }
        }
    }

    private UtilisateurResponse versReponse(UserRep u) {
        Profil profil = keycloak.rolesUtilisateur(u.id()).stream()
                .map(RoleRep::name).filter(PROFILS::contains).findFirst()
                .map(Profil::valueOf).orElse(null);
        List<String> projets = keycloak.groupesUtilisateur(u.id()).stream().map(GroupRep::name)
                .filter(n -> n.startsWith(PREFIXE_PROJET)).map(n -> n.substring(PREFIXE_PROJET.length()))
                .sorted().toList();
        return new UtilisateurResponse(u.id(), u.username(), u.firstName(), u.lastName(), u.email(),
                Boolean.TRUE.equals(u.enabled()), profil, projets);
    }

    private void tracer(String evenement, String details) {
        journalService.tracer(evenement, details + " [par " + currentUser.username() + "]");
    }

    /** 12 caractères, sans caractères ambigus (0/O, 1/l). */
    private static String motDePasseTemporaire() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder("Ahi-");
        for (int i = 0; i < 8; i++) {
            sb.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}

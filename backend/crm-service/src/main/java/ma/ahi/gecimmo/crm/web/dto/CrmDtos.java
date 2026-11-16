package ma.ahi.gecimmo.crm.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import ma.ahi.gecimmo.crm.domain.Prospect;
import ma.ahi.gecimmo.crm.domain.ProspectStatut;
import ma.ahi.gecimmo.crm.domain.Relance;
import ma.ahi.gecimmo.crm.domain.RendezVous;
import ma.ahi.gecimmo.crm.domain.SourceProspect;

public final class CrmDtos {

    private CrmDtos() {
    }

    // ---------- Prospects ----------
    public record ProspectRequest(
            @NotBlank String nom,
            String prenom,
            @NotBlank @Pattern(regexp = "^[+0-9 ]{9,20}$", message = "Téléphone invalide") String telephone,
            @NotBlank @Pattern(regexp = "^[A-Za-z]{1,2}[0-9]{3,8}$", message = "CIN invalide (ex. BE123456)") String cin,
            @Email String email,
            @NotNull SourceProspect source,
            @NotNull @DecimalMin("0.0") BigDecimal budget,
            @NotBlank String typologieRecherchee,
            @NotBlank String projetCode) {
    }

    public record ProspectResponse(Long id, String nom, String prenom, String telephone, String cin, String email,
                                   SourceProspect source, BigDecimal budget, String typologieRecherchee,
                                   String projetCode, String commercial, ProspectStatut statut, String motifPerte,
                                   LocalDateTime dateCreation) {
        public static ProspectResponse of(Prospect p) {
            return new ProspectResponse(p.getId(), p.getNom(), p.getPrenom(), p.getTelephone(), p.getCin(),
                    p.getEmail(), p.getSource(), p.getBudget(), p.getTypologieRecherchee(), p.getProjetCode(),
                    p.getCommercial(), p.getStatut(), p.getMotifPerte(), p.getDateCreation());
        }
    }

    public record PerteRequest(@NotBlank String motif) {
    }

    public record ReaffectationRequest(@NotBlank String ancienCommercial, @NotBlank String nouveauCommercial) {
    }

    public record ReaffectationResponse(int prospects, int relances) {
    }

    // ---------- Rendez-vous ----------
    public record RendezVousRequest(@NotNull Long prospectId, @NotNull LocalDateTime dateHeure,
                                    @NotNull RendezVous.Type type, Long lotId) {
    }

    public record CompteRenduRequest(@NotBlank String compteRendu) {
    }

    public record RendezVousResponse(Long id, Long prospectId, String prospectNom, LocalDateTime dateHeure,
                                     RendezVous.Type type, Long lotId, RendezVous.Statut statut,
                                     String compteRendu, LocalDateTime dateCompteRendu,
                                     boolean compteRenduHorsDelai, String commercial) {
        public static RendezVousResponse of(RendezVous r) {
            return new RendezVousResponse(r.getId(), r.getProspect().getId(), r.getProspect().nomComplet(),
                    r.getDateHeure(), r.getType(), r.getLotId(), r.getStatut(), r.getCompteRendu(),
                    r.getDateCompteRendu(), r.isCompteRenduHorsDelai(), r.getCommercial());
        }
    }

    // ---------- Relances ----------
    public record RelanceRequest(@NotNull Long prospectId, @NotNull LocalDate dateRelance, @NotBlank String objet) {
    }

    public record RelanceFaiteRequest(String resultat) {
    }

    public record RelanceResponse(Long id, Long prospectId, String prospectNom, String telephone,
                                  LocalDate dateRelance, String objet, boolean faite, LocalDateTime dateFaite,
                                  String resultat, String commercial) {
        public static RelanceResponse of(Relance r) {
            return new RelanceResponse(r.getId(), r.getProspect().getId(), r.getProspect().nomComplet(),
                    r.getProspect().getTelephone(), r.getDateRelance(), r.getObjet(), r.isFaite(),
                    r.getDateFaite(), r.getResultat(), r.getCommercial());
        }
    }

    // ---------- Indicateurs ----------
    public record IndicateursCommercial(String commercial, String mois, long visitesEffectuees,
                                        long prospectsConvertis, double tauxTransformation) {
    }
}

package ma.ahi.gecimmo.referentiel.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import ma.ahi.gecimmo.referentiel.domain.DemandePrix;
import ma.ahi.gecimmo.referentiel.domain.Immeuble;
import ma.ahi.gecimmo.referentiel.domain.Lot;
import ma.ahi.gecimmo.referentiel.domain.LotStatut;
import ma.ahi.gecimmo.referentiel.domain.Projet;
import ma.ahi.gecimmo.referentiel.domain.ProjetStatut;
import ma.ahi.gecimmo.referentiel.domain.Tranche;
import ma.ahi.gecimmo.referentiel.domain.TypeBatiment;
import ma.ahi.gecimmo.referentiel.domain.Typologie;

/** DTO (objets échangés en JSON) : on n'expose jamais les entités JPA directement. */
public final class ReferentielDtos {

    private ReferentielDtos() {
    }

    // ---------- Projets ----------
    public record ProjetRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z]{2,10}", message = "Code projet : 2 à 10 lettres") String code,
            @NotBlank String nom,
            @NotBlank String ville,
            String type,
            @NotNull ProjetStatut statut) {
    }

    public record ProjetResponse(Long id, String code, String nom, String ville, String type, ProjetStatut statut) {
        public static ProjetResponse of(Projet p) {
            return new ProjetResponse(p.getId(), p.getCode(), p.getNom(), p.getVille(), p.getType(), p.getStatut());
        }
    }

    // ---------- Tranches ----------
    public record TrancheRequest(@NotBlank @Size(max = 10) String code, String nom) {
    }

    public record TrancheResponse(Long id, String code, String nom, String projetCode) {
        public static TrancheResponse of(Tranche t) {
            return new TrancheResponse(t.getId(), t.getCode(), t.getNom(), t.getProjet().getCode());
        }
    }

    // ---------- Immeubles / îlots ----------
    public record ImmeubleRequest(@NotBlank @Size(max = 10) String code, @NotNull TypeBatiment type,
                                  @Min(0) Integer nombreEtages) {
    }

    public record ImmeubleResponse(Long id, String code, TypeBatiment type, Integer nombreEtages, Long trancheId) {
        public static ImmeubleResponse of(Immeuble i) {
            return new ImmeubleResponse(i.getId(), i.getCode(), i.getType(), i.getNombreEtages(),
                    i.getTranche().getId());
        }
    }

    // ---------- Lots ----------
    public record LotRequest(
            @NotBlank @Size(max = 10) String numero,
            @Min(0) int etage,
            @NotNull Typologie typologie,
            @DecimalMin("0.0") BigDecimal surface,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal prixTTC) {
    }

    /** Mise à jour descriptive (le prix passe par une demande validée par la direction). */
    public record LotUpdateRequest(@NotNull Typologie typologie, @DecimalMin("0.0") BigDecimal surface) {
    }

    public record LotResponse(Long id, String code, String projetCode, String trancheCode, String immeubleCode,
                              int etage, String numero, Typologie typologie, BigDecimal surface,
                              BigDecimal prixTTC, LotStatut statut, LocalDateTime dateMajStatut) {
        public static LotResponse of(Lot l) {
            Immeuble imm = l.getImmeuble();
            return new LotResponse(l.getId(), l.getCode(), l.getProjetCode(), imm.getTranche().getCode(),
                    imm.getCode(), l.getEtage(), l.getNumero(), l.getTypologie(), l.getSurface(),
                    l.getPrixTTC(), l.getStatut(), l.getDateMajStatut());
        }
    }

    public record StockResponse(String projetCode, long total, Map<LotStatut, Long> parStatut) {
    }

    // ---------- Grille de prix ----------
    public record DemandePrixRequest(
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal nouveauPrix,
            @NotBlank String motif) {
    }

    public record DemandePrixResponse(Long id, Long lotId, String lotCode, BigDecimal ancienPrix,
                                      BigDecimal nouveauPrix, String motif, DemandePrix.Statut statut,
                                      String demandeur, LocalDateTime dateDemande, String validateur,
                                      LocalDateTime dateDecision) {
        public static DemandePrixResponse of(DemandePrix d) {
            return new DemandePrixResponse(d.getId(), d.getLot().getId(), d.getLot().getCode(), d.getAncienPrix(),
                    d.getNouveauPrix(), d.getMotif(), d.getStatut(), d.getDemandeur(), d.getDateDemande(),
                    d.getValidateur(), d.getDateDecision());
        }
    }
}

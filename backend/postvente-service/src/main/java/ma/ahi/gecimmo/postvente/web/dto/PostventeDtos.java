package ma.ahi.gecimmo.postvente.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import ma.ahi.gecimmo.postvente.domain.DossierNotaire;
import ma.ahi.gecimmo.postvente.domain.Intervention;
import ma.ahi.gecimmo.postvente.domain.Livraison;
import ma.ahi.gecimmo.postvente.domain.TicketSav;

public final class PostventeDtos {

    private PostventeDtos() {
    }

    // ---------- Notaire ----------
    public record DossierNotaireRequest(@NotNull Long reservationId, @NotBlank String notaire, String observations) {
    }

    public record TransmissionRequest(@NotNull LocalDate dateTransmission, String observations) {
    }

    public record ActeRequest(@NotBlank String numeroActe, @NotNull LocalDate dateSignature) {
    }

    public record DossierNotaireResponse(Long id, Long reservationId, String reservationNumero, Long lotId,
                                         String lotCode, String projetCode, String clientNom, String notaire,
                                         String observations, DossierNotaire.Statut statut,
                                         LocalDateTime dateOuverture, LocalDate dateTransmission,
                                         LocalDate dateSignatureActe, String numeroActe, String ouvertPar,
                                         BigDecimal resteAPayer) {
        public static DossierNotaireResponse of(DossierNotaire d, BigDecimal resteAPayer) {
            return new DossierNotaireResponse(d.getId(), d.getReservationId(), d.getReservationNumero(), d.getLotId(),
                    d.getLotCode(), d.getProjetCode(), d.getClientNom(), d.getNotaire(), d.getObservations(),
                    d.getStatut(), d.getDateOuverture(), d.getDateTransmission(), d.getDateSignatureActe(),
                    d.getNumeroActe(), d.getOuvertPar(), resteAPayer);
        }
    }

    // ---------- Livraison ----------
    public record PlanificationRequest(@NotNull Long reservationId, @NotNull LocalDate datePrevue) {
    }

    public record ReplanificationRequest(@NotNull LocalDate datePrevue) {
    }

    public record PreReceptionRequest(@NotNull LocalDate date, String reserves) {
    }

    public record PvRequest(@NotNull LocalDate dateLivraison, @Min(1) int nombreCles) {
    }

    public record LivraisonResponse(Long id, Long reservationId, String reservationNumero, Long lotId, String lotCode,
                                    String projetCode, String clientNom, String clientTelephone,
                                    LocalDate datePrevue, Livraison.Statut statut, LocalDate datePreReception,
                                    String reserves, boolean reservesLevees, LocalDate dateLivraison,
                                    String livrePar, boolean clesRemises, int nombreCles, LocalDate finGarantieSav) {
        public static LivraisonResponse of(Livraison l) {
            return new LivraisonResponse(l.getId(), l.getReservationId(), l.getReservationNumero(), l.getLotId(),
                    l.getLotCode(), l.getProjetCode(), l.getClientNom(), l.getClientTelephone(), l.getDatePrevue(),
                    l.getStatut(), l.getDatePreReception(), l.getReserves(), l.isReservesLevees(),
                    l.getDateLivraison(), l.getLivrePar(), l.isClesRemises(), l.getNombreCles(),
                    l.getDateLivraison() == null ? null : l.getDateLivraison().plusMonths(Livraison.DUREE_SAV_MOIS));
        }
    }

    // ---------- SAV ----------
    public record TicketRequest(@NotNull Long livraisonId, @NotNull TicketSav.Categorie categorie,
                                @NotNull TicketSav.Priorite priorite, @NotBlank String description) {
    }

    public record InterventionRequest(@NotNull LocalDateTime dateIntervention, @NotBlank String intervenant,
                                      @NotBlank String travaux) {
    }

    public record ClotureRequest(String commentaire) {
    }

    public record InterventionResponse(Long id, LocalDateTime dateIntervention, String intervenant, String travaux) {
        public static InterventionResponse of(Intervention i) {
            return new InterventionResponse(i.getId(), i.getDateIntervention(), i.getIntervenant(), i.getTravaux());
        }
    }

    public record TicketResponse(Long id, String numero, Long livraisonId, String lotCode, String projetCode,
                                 String clientNom, String clientTelephone, TicketSav.Categorie categorie,
                                 TicketSav.Priorite priorite, String description, TicketSav.Statut statut,
                                 LocalDateTime dateOuverture, String ouvertPar, LocalDateTime dateCloture,
                                 String commentaireCloture, List<InterventionResponse> interventions) {
        public static TicketResponse of(TicketSav t) {
            Livraison l = t.getLivraison();
            return new TicketResponse(t.getId(), t.getNumero(), l.getId(), l.getLotCode(), l.getProjetCode(),
                    l.getClientNom(), l.getClientTelephone(), t.getCategorie(), t.getPriorite(), t.getDescription(),
                    t.getStatut(), t.getDateOuverture(), t.getOuvertPar(), t.getDateCloture(),
                    t.getCommentaireCloture(), t.getInterventions().stream().map(InterventionResponse::of).toList());
        }
    }
}

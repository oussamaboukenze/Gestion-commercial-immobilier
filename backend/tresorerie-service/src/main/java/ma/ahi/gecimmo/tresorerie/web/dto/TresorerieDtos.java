package ma.ahi.gecimmo.tresorerie.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import ma.ahi.gecimmo.tresorerie.domain.CreditBancaire;
import ma.ahi.gecimmo.tresorerie.domain.DemandeCorrection;
import ma.ahi.gecimmo.tresorerie.domain.Encaissement;
import ma.ahi.gecimmo.tresorerie.domain.Impaye;
import ma.ahi.gecimmo.tresorerie.domain.ModePaiement;
import ma.ahi.gecimmo.tresorerie.domain.RelanceClient;
import ma.ahi.gecimmo.tresorerie.domain.RemiseEnBanque;

public final class TresorerieDtos {

    private TresorerieDtos() {
    }

    // ---------- Encaissements ----------
    public record EncaissementRequest(
            @NotNull Long reservationId,
            Long echeanceId,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal montant,
            @NotNull ModePaiement mode,
            String numeroCheque,
            String banque,
            String reference,
            @NotNull LocalDate dateEncaissement) {
    }

    public record EncaissementResponse(Long id, String numeroRecu, Long reservationId, String reservationNumero,
                                       String clientNom, String projetCode, Long echeanceId, BigDecimal montant,
                                       ModePaiement mode, String numeroCheque, String banque, String reference,
                                       LocalDate dateEncaissement, Encaissement.Statut statut,
                                       Encaissement.Origine origine, String remiseNumero, String saisiPar,
                                       LocalDateTime dateSaisie) {
        public static EncaissementResponse of(Encaissement e) {
            return new EncaissementResponse(e.getId(), e.getNumeroRecu(), e.getReservationId(),
                    e.getReservationNumero(), e.getClientNom(), e.getProjetCode(), e.getEcheanceId(), e.getMontant(),
                    e.getMode(), e.getNumeroCheque(), e.getBanque(), e.getReference(), e.getDateEncaissement(),
                    e.getStatut(), e.getOrigine(), e.getRemise() == null ? null : e.getRemise().getNumero(),
                    e.getSaisiPar(), e.getDateSaisie());
        }
    }

    // ---------- Remises en banque ----------
    public record RemiseRequest(@NotBlank String banque, @NotNull LocalDate dateRemise,
                                @NotEmpty List<Long> encaissementIds) {
    }

    public record RemiseResponse(Long id, String numero, String banque, LocalDate dateRemise,
                                 BigDecimal montantTotal, int nombreCheques, List<EncaissementResponse> cheques) {
        public static RemiseResponse of(RemiseEnBanque r) {
            List<EncaissementResponse> cheques = r.getCheques().stream().map(EncaissementResponse::of).toList();
            return new RemiseResponse(r.getId(), r.getNumero(), r.getBanque(), r.getDateRemise(),
                    r.getMontantTotal(), cheques.size(), cheques);
        }
    }

    // ---------- Impayés ----------
    public record ImpayeRequest(@NotNull Long encaissementId, @NotBlank String motif, @NotNull LocalDate dateRejet) {
    }

    public record ImpayeResponse(Long id, Long encaissementId, String numeroRecu, String clientNom,
                                 String reservationNumero, BigDecimal montant, String numeroCheque, String motif,
                                 LocalDate dateRejet, String saisiPar) {
        public static ImpayeResponse of(Impaye i) {
            Encaissement e = i.getEncaissement();
            return new ImpayeResponse(i.getId(), e.getId(), e.getNumeroRecu(), e.getClientNom(),
                    e.getReservationNumero(), e.getMontant(), e.getNumeroCheque(), i.getMotif(), i.getDateRejet(),
                    i.getSaisiPar());
        }
    }

    // ---------- Corrections (contre-passation) ----------
    public record CorrectionRequest(@NotBlank String motif) {
    }

    public record CorrectionResponse(Long id, Long encaissementId, String numeroRecu, String clientNom,
                                     BigDecimal montant, String motif, DemandeCorrection.Statut statut,
                                     String demandeur, LocalDateTime dateDemande, String validateurDaf,
                                     LocalDateTime dateDecision) {
        public static CorrectionResponse of(DemandeCorrection d) {
            Encaissement e = d.getEncaissement();
            return new CorrectionResponse(d.getId(), e.getId(), e.getNumeroRecu(), e.getClientNom(), e.getMontant(),
                    d.getMotif(), d.getStatut(), d.getDemandeur(), d.getDateDemande(), d.getValidateurDaf(),
                    d.getDateDecision());
        }
    }

    // ---------- Relances ----------
    public record RelanceResponse(Long id, Long echeanceId, Long reservationId, String reservationNumero,
                                  String clientNom, String projetCode, String objet, BigDecimal montantDu,
                                  LocalDate dateEcheance, RelanceClient.Niveau niveau, RelanceClient.Statut statut,
                                  LocalDateTime dateCreation, LocalDateTime dateEnvoi, String valideeParDaf) {
        public static RelanceResponse of(RelanceClient r) {
            return new RelanceResponse(r.getId(), r.getEcheanceId(), r.getReservationId(), r.getReservationNumero(),
                    r.getClientNom(), r.getProjetCode(), r.getObjet(), r.getMontantDu(), r.getDateEcheance(),
                    r.getNiveau(), r.getStatut(), r.getDateCreation(), r.getDateEnvoi(), r.getValideeParDaf());
        }
    }

    // ---------- Crédits bancaires ----------
    public record CreditRequest(@NotNull Long reservationId, @NotBlank String banque,
                                @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal montantDemande,
                                @NotNull LocalDate dateDemande) {
    }

    public record AccordCreditRequest(boolean accorde, @DecimalMin("0.0") BigDecimal montantAccorde,
                                      @NotNull LocalDate date) {
    }

    public record DeblocageRequest(@NotNull LocalDate dateDeblocage, @NotBlank String reference) {
    }

    public record CreditResponse(Long id, Long reservationId, String reservationNumero, String clientNom,
                                 String projetCode, String banque, BigDecimal montantDemande,
                                 BigDecimal montantAccorde, CreditBancaire.Statut statut, LocalDate dateDemande,
                                 LocalDate dateAccord, LocalDate dateDeblocage, Long encaissementId) {
        public static CreditResponse of(CreditBancaire c) {
            return new CreditResponse(c.getId(), c.getReservationId(), c.getReservationNumero(), c.getClientNom(),
                    c.getProjetCode(), c.getBanque(), c.getMontantDemande(), c.getMontantAccorde(), c.getStatut(),
                    c.getDateDemande(), c.getDateAccord(), c.getDateDeblocage(), c.getEncaissementId());
        }
    }
}

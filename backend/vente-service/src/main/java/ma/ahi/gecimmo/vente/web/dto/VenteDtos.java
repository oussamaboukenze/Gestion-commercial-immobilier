package ma.ahi.gecimmo.vente.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import ma.ahi.gecimmo.vente.domain.ChangementLot;
import ma.ahi.gecimmo.vente.domain.Compromis;
import ma.ahi.gecimmo.vente.domain.Desistement;
import ma.ahi.gecimmo.vente.domain.Echeance;
import ma.ahi.gecimmo.vente.domain.OptionLot;
import ma.ahi.gecimmo.vente.domain.RegleVente;
import ma.ahi.gecimmo.vente.domain.Reservation;
import ma.ahi.gecimmo.vente.domain.ReservationStatut;
import ma.ahi.gecimmo.vente.domain.StatutRemise;

public final class VenteDtos {

    private VenteDtos() {
    }

    // ---------- Options ----------
    public record OptionRequest(@NotNull Long lotId, @NotNull Long prospectId) {
    }

    public record OptionResponse(Long id, Long lotId, String lotCode, String projetCode, Long prospectId,
                                 String commercial, LocalDateTime dateDebut, LocalDateTime dateExpiration,
                                 OptionLot.Statut statut) {
        public static OptionResponse of(OptionLot o) {
            return new OptionResponse(o.getId(), o.getLotId(), o.getLotCode(), o.getProjetCode(), o.getProspectId(),
                    o.getCommercial(), o.getDateDebut(), o.getDateExpiration(), o.getStatut());
        }
    }

    // ---------- Réservations ----------
    public record ReservationRequest(
            @NotNull Long lotId,
            @NotNull Long prospectId,
            @DecimalMin("0.0") @DecimalMax("99.99") BigDecimal remisePct,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal avance,
            boolean pieceCin,
            boolean pieceJustificatifRevenus,
            boolean pieceRecuAvance) {
    }

    public record PiecesRequest(boolean pieceCin, boolean pieceJustificatifRevenus, boolean pieceRecuAvance) {
    }

    public record MotifRequest(@NotBlank String motif) {
    }

    public record ReservationResponse(Long id, String numero, Long lotId, String lotCode, String projetCode,
                                      Long prospectId, String clientNom, String clientCin, String clientTelephone,
                                      String commercial, BigDecimal prixTTC, BigDecimal remisePct,
                                      BigDecimal prixNet, StatutRemise statutRemise, BigDecimal avance,
                                      BigDecimal avanceMinimale, BigDecimal totalEncaisse, boolean avanceConfirmee,
                                      boolean pieceCin, boolean pieceJustificatifRevenus, boolean pieceRecuAvance,
                                      ReservationStatut statut, LocalDateTime dateCreation,
                                      LocalDateTime dateValidation, String validateurAdv,
                                      LocalDate dateLimiteCompromis, String motifRejet) {
        public static ReservationResponse of(Reservation r) {
            return new ReservationResponse(r.getId(), r.getNumero(), r.getLotId(), r.getLotCode(), r.getProjetCode(),
                    r.getProspectId(), r.getClientNom(), r.getClientCin(), r.getClientTelephone(),
                    r.getCommercial(), r.getPrixTTC(), r.getRemisePct(), r.getPrixNet(), r.getStatutRemise(),
                    r.getAvance(), RegleVente.avanceMinimale(r.getPrixNet()), r.getTotalEncaisse(),
                    r.avanceConfirmee(), r.isPieceCin(), r.isPieceJustificatifRevenus(), r.isPieceRecuAvance(),
                    r.getStatut(), r.getDateCreation(), r.getDateValidation(), r.getValidateurAdv(),
                    r.getDateLimiteCompromis(), r.getMotifRejet());
        }
    }

    // ---------- Compromis & échéancier ----------
    public record CompromisRequest(@NotNull Long reservationId) {
    }

    public record EcheanceResponse(Long id, int ordre, String libelle, BigDecimal pourcentage, BigDecimal montant,
                                   BigDecimal montantPaye, BigDecimal resteAPayer, LocalDate dateEcheance,
                                   Echeance.Etat etat) {
        public static EcheanceResponse of(Echeance e) {
            return new EcheanceResponse(e.getId(), e.getOrdre(), e.getLibelle(), e.getPourcentage(), e.getMontant(),
                    e.getMontantPaye(), e.getResteAPayer(), e.getDateEcheance(), e.etat(LocalDate.now()));
        }
    }

    public record CompromisResponse(Long id, Long reservationId, String reservationNumero, String clientNom,
                                    String lotCode, BigDecimal prixNet, BigDecimal totalEncaisse,
                                    BigDecimal versementRequisSignature, Compromis.Statut statut,
                                    LocalDateTime dateCreation, LocalDateTime dateSignature,
                                    List<EcheanceResponse> echeances) {
        public static CompromisResponse of(Compromis c) {
            Reservation r = c.getReservation();
            return new CompromisResponse(c.getId(), r.getId(), r.getNumero(), r.getClientNom(), r.getLotCode(),
                    r.getPrixNet(), r.getTotalEncaisse(), RegleVente.versementMinimalCompromis(r.getPrixNet()),
                    c.getStatut(), c.getDateCreation(), c.getDateSignature(),
                    c.getEcheances().stream().map(EcheanceResponse::of).toList());
        }
    }

    public record DateEcheanceRequest(@NotNull LocalDate dateEcheance) {
    }

    // ---------- Désistements ----------
    public record DesistementRequest(@NotNull Long reservationId, @NotBlank String motif,
                                     @DecimalMin("0.0") BigDecimal montantSelonClauses) {
    }

    public record DesistementResponse(Long id, Long reservationId, String reservationNumero, String clientNom,
                                      String lotCode, String motif, boolean apresCompromis, BigDecimal totalVerse,
                                      BigDecimal fraisDossier, BigDecimal montantARembourser,
                                      Desistement.Statut statut, String saisiPar, LocalDateTime dateSaisie,
                                      String validateurRespCommercial, String validateurDaf,
                                      LocalDateTime dateDecision, String motifRefus) {
        public static DesistementResponse of(Desistement d) {
            Reservation r = d.getReservation();
            return new DesistementResponse(d.getId(), r.getId(), r.getNumero(), r.getClientNom(), r.getLotCode(),
                    d.getMotif(), d.isApresCompromis(), d.getTotalVerse(), d.getFraisDossier(),
                    d.getMontantARembourser(), d.getStatut(), d.getSaisiPar(), d.getDateSaisie(),
                    d.getValidateurRespCommercial(), d.getValidateurDaf(), d.getDateDecision(), d.getMotifRefus());
        }
    }

    // ---------- Changements de lot ----------
    public record ChangementLotRequest(@NotNull Long reservationId, @NotNull Long nouveauLotId, String motif) {
    }

    public record ChangementLotResponse(Long id, Long reservationId, String reservationNumero, String clientNom,
                                        String ancienLotCode, BigDecimal ancienPrix, String nouveauLotCode,
                                        BigDecimal nouveauPrix, String motif, ChangementLot.Statut statut,
                                        String saisiPar, LocalDateTime dateSaisie, String validateur,
                                        LocalDateTime dateDecision) {
        public static ChangementLotResponse of(ChangementLot c) {
            Reservation r = c.getReservation();
            return new ChangementLotResponse(c.getId(), r.getId(), r.getNumero(), r.getClientNom(),
                    c.getAncienLotCode(), c.getAncienPrix(), c.getNouveauLotCode(), c.getNouveauPrix(), c.getMotif(),
                    c.getStatut(), c.getSaisiPar(), c.getDateSaisie(), c.getValidateur(), c.getDateDecision());
        }
    }

    // ---------- Reporting ----------
    public record LigneEtatVentes(long nombre, BigDecimal montant) {
    }

    public record EtatVentesResponse(String projetCode, Map<ReservationStatut, LigneEtatVentes> parStatut,
                                     BigDecimal caReserveActif) {
    }

    public record ChiffresCommercialResponse(String commercial, String mois, BigDecimal caReserve) {
    }
}

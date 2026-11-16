package ma.ahi.gecimmo.vente.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Désistement (annulation de vente). Saisi par l'ADV, validé par le responsable commercial,
 * puis par le DAF si le compromis est signé.
 */
@Entity
@Table(name = "desistements")
public class Desistement {

    public enum Statut { EN_ATTENTE_RESP_COMMERCIAL, EN_ATTENTE_DAF, VALIDE, REFUSE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @Column(nullable = false, length = 1000)
    private String motif;

    /** true si le compromis était signé au moment du désistement. */
    private boolean apresCompromis;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalVerse;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal fraisDossier;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal montantARembourser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Statut statut = Statut.EN_ATTENTE_RESP_COMMERCIAL;

    /** Statut de la réservation avant désistement (pour remettre le lot à disposition). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReservationStatut statutReservationInitial;

    private String saisiPar;
    private LocalDateTime dateSaisie;
    private String validateurRespCommercial;
    private String validateurDaf;
    private LocalDateTime dateDecision;
    private String motifRefus;

    protected Desistement() {
    }

    /**
     * @param montantSelonClauses après compromis : montant à rembourser selon les clauses du compromis
     *                            (saisi par l'ADV) ; ignoré avant compromis.
     */
    public Desistement(Reservation reservation, String motif, BigDecimal montantSelonClauses, String saisiPar) {
        this.reservation = reservation;
        this.motif = motif;
        this.statutReservationInitial = reservation.getStatut();
        this.apresCompromis = reservation.getStatut() == ReservationStatut.COMPROMIS_SIGNE;
        this.totalVerse = reservation.getTotalEncaisse();
        if (apresCompromis) {
            if (montantSelonClauses == null || montantSelonClauses.signum() < 0
                    || montantSelonClauses.compareTo(totalVerse) > 0) {
                throw new IllegalArgumentException(
                        "Après compromis, saisissez le montant à rembourser selon les clauses (entre 0 et "
                                + totalVerse + " DH).");
            }
            this.montantARembourser = RegleVente.arrondir(montantSelonClauses);
            this.fraisDossier = RegleVente.arrondir(totalVerse.subtract(montantARembourser));
        } else {
            this.fraisDossier = RegleVente.fraisDossierDesistement(totalVerse);
            this.montantARembourser = RegleVente.arrondir(totalVerse.subtract(fraisDossier));
        }
        this.saisiPar = saisiPar;
        this.dateSaisie = LocalDateTime.now();
    }

    public void validerParRespCommercial(String validateur) {
        this.validateurRespCommercial = validateur;
        if (apresCompromis) {
            this.statut = Statut.EN_ATTENTE_DAF;
        } else {
            this.statut = Statut.VALIDE;
            this.dateDecision = LocalDateTime.now();
        }
    }

    public void validerParDaf(String validateur) {
        this.validateurDaf = validateur;
        this.statut = Statut.VALIDE;
        this.dateDecision = LocalDateTime.now();
    }

    public void refuser(String validateur, String motifRefus) {
        if (statut == Statut.EN_ATTENTE_DAF) {
            this.validateurDaf = validateur;
        } else {
            this.validateurRespCommercial = validateur;
        }
        this.statut = Statut.REFUSE;
        this.motifRefus = motifRefus;
        this.dateDecision = LocalDateTime.now();
    }

    public boolean estEnAttente() {
        return statut == Statut.EN_ATTENTE_RESP_COMMERCIAL || statut == Statut.EN_ATTENTE_DAF;
    }

    public Long getId() { return id; }
    public Reservation getReservation() { return reservation; }
    public String getMotif() { return motif; }
    public boolean isApresCompromis() { return apresCompromis; }
    public BigDecimal getTotalVerse() { return totalVerse; }
    public BigDecimal getFraisDossier() { return fraisDossier; }
    public BigDecimal getMontantARembourser() { return montantARembourser; }
    public Statut getStatut() { return statut; }
    public ReservationStatut getStatutReservationInitial() { return statutReservationInitial; }
    public String getSaisiPar() { return saisiPar; }
    public LocalDateTime getDateSaisie() { return dateSaisie; }
    public String getValidateurRespCommercial() { return validateurRespCommercial; }
    public String getValidateurDaf() { return validateurDaf; }
    public LocalDateTime getDateDecision() { return dateDecision; }
    public String getMotifRefus() { return motifRefus; }
}

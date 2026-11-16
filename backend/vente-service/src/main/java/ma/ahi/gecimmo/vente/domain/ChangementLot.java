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

/** Changement de lot : saisi par l'ADV, validé par le responsable commercial. */
@Entity
@Table(name = "changements_lot")
public class ChangementLot {

    public enum Statut { EN_ATTENTE, VALIDE, REFUSE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @Column(nullable = false)
    private Long ancienLotId;
    @Column(nullable = false, length = 40)
    private String ancienLotCode;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal ancienPrix;

    @Column(nullable = false)
    private Long nouveauLotId;
    @Column(nullable = false, length = 40)
    private String nouveauLotCode;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal nouveauPrix;

    @Column(length = 1000)
    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.EN_ATTENTE;

    private String saisiPar;
    private LocalDateTime dateSaisie;
    private String validateur;
    private LocalDateTime dateDecision;

    protected ChangementLot() {
    }

    public ChangementLot(Reservation reservation, Long nouveauLotId, String nouveauLotCode, BigDecimal nouveauPrix,
                         String motif, String saisiPar) {
        this.reservation = reservation;
        this.ancienLotId = reservation.getLotId();
        this.ancienLotCode = reservation.getLotCode();
        this.ancienPrix = reservation.getPrixTTC();
        this.nouveauLotId = nouveauLotId;
        this.nouveauLotCode = nouveauLotCode;
        this.nouveauPrix = nouveauPrix;
        this.motif = motif;
        this.saisiPar = saisiPar;
        this.dateSaisie = LocalDateTime.now();
    }

    public void decider(boolean valide, String validateur) {
        this.statut = valide ? Statut.VALIDE : Statut.REFUSE;
        this.validateur = validateur;
        this.dateDecision = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Reservation getReservation() { return reservation; }
    public Long getAncienLotId() { return ancienLotId; }
    public String getAncienLotCode() { return ancienLotCode; }
    public BigDecimal getAncienPrix() { return ancienPrix; }
    public Long getNouveauLotId() { return nouveauLotId; }
    public String getNouveauLotCode() { return nouveauLotCode; }
    public BigDecimal getNouveauPrix() { return nouveauPrix; }
    public String getMotif() { return motif; }
    public Statut getStatut() { return statut; }
    public String getSaisiPar() { return saisiPar; }
    public LocalDateTime getDateSaisie() { return dateSaisie; }
    public String getValidateur() { return validateur; }
    public LocalDateTime getDateDecision() { return dateDecision; }
}

package ma.ahi.gecimmo.tresorerie.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Encaissement d'un client. Règle AHI : un encaissement validé ne se supprime jamais ;
 * on le corrige par contre-passation (validation DAF) ou on le déclare impayé (chèque rejeté).
 */
@Entity
@Table(name = "encaissements", indexes = {
        @Index(name = "idx_enc_reservation", columnList = "reservation_id"),
        @Index(name = "idx_enc_date", columnList = "date_encaissement")})
public class Encaissement {

    public enum Statut { VALIDE, IMPAYE, CONTRE_PASSE }

    public enum Origine { CLIENT, CREDIT_BANCAIRE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** N° du reçu : REC-2026-000001 */
    @Column(unique = true, length = 20)
    private String numeroRecu;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;
    @Column(nullable = false, length = 20)
    private String reservationNumero;
    @Column(nullable = false)
    private String clientNom;
    @Column(nullable = false, length = 10)
    private String projetCode;

    /** Echéance visée (information) ; vente-service répartit les paiements dans l'ordre des échéances. */
    private Long echeanceId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModePaiement mode;

    private String numeroCheque;
    private String banque;
    private String reference;

    @Column(name = "date_encaissement", nullable = false)
    private LocalDate dateEncaissement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.VALIDE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Origine origine = Origine.CLIENT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remise_id")
    private RemiseEnBanque remise;

    private String saisiPar;
    private LocalDateTime dateSaisie;
    /** Date du rejet (impayé) ou de la contre-passation. */
    private LocalDate dateAnnulation;

    @Version
    private Long version;

    protected Encaissement() {
    }

    public Encaissement(Long reservationId, String reservationNumero, String clientNom, String projetCode,
                        Long echeanceId, BigDecimal montant, ModePaiement mode, String numeroCheque, String banque,
                        String reference, LocalDate dateEncaissement, Origine origine, String saisiPar) {
        this.reservationId = reservationId;
        this.reservationNumero = reservationNumero;
        this.clientNom = clientNom;
        this.projetCode = projetCode;
        this.echeanceId = echeanceId;
        this.montant = montant;
        this.mode = mode;
        this.numeroCheque = numeroCheque;
        this.banque = banque;
        this.reference = reference;
        this.dateEncaissement = dateEncaissement;
        this.origine = origine;
        this.saisiPar = saisiPar;
        this.dateSaisie = LocalDateTime.now();
    }

    public void attribuerNumero() {
        this.numeroRecu = String.format("REC-%d-%06d", dateSaisie.getYear(), id);
    }

    public void declarerImpaye(LocalDate date) {
        this.statut = Statut.IMPAYE;
        this.dateAnnulation = date;
    }

    public void contrePasser(LocalDate date) {
        this.statut = Statut.CONTRE_PASSE;
        this.dateAnnulation = date;
    }

    public void affecterRemise(RemiseEnBanque remise) {
        this.remise = remise;
    }

    public Long getId() { return id; }
    public String getNumeroRecu() { return numeroRecu; }
    public Long getReservationId() { return reservationId; }
    public String getReservationNumero() { return reservationNumero; }
    public String getClientNom() { return clientNom; }
    public String getProjetCode() { return projetCode; }
    public Long getEcheanceId() { return echeanceId; }
    public BigDecimal getMontant() { return montant; }
    public ModePaiement getMode() { return mode; }
    public String getNumeroCheque() { return numeroCheque; }
    public String getBanque() { return banque; }
    public String getReference() { return reference; }
    public LocalDate getDateEncaissement() { return dateEncaissement; }
    public Statut getStatut() { return statut; }
    public Origine getOrigine() { return origine; }
    public RemiseEnBanque getRemise() { return remise; }
    public String getSaisiPar() { return saisiPar; }
    public LocalDateTime getDateSaisie() { return dateSaisie; }
    public LocalDate getDateAnnulation() { return dateAnnulation; }
}

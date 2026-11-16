package ma.ahi.gecimmo.tresorerie.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Crédit bancaire du client : suivi accord / déblocage. */
@Entity
@Table(name = "credits_bancaires")
public class CreditBancaire {

    public enum Statut { DEMANDE, ACCORDE, REFUSE, DEBLOQUE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reservationId;
    @Column(nullable = false, length = 20)
    private String reservationNumero;
    @Column(nullable = false)
    private String clientNom;
    @Column(nullable = false, length = 10)
    private String projetCode;

    @Column(nullable = false)
    private String banque;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal montantDemande;
    @Column(precision = 14, scale = 2)
    private BigDecimal montantAccorde;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.DEMANDE;

    private LocalDate dateDemande;
    private LocalDate dateAccord;
    private LocalDate dateDeblocage;
    /** Encaissement créé au déblocage. */
    private Long encaissementId;

    protected CreditBancaire() {
    }

    public CreditBancaire(Long reservationId, String reservationNumero, String clientNom, String projetCode,
                          String banque, BigDecimal montantDemande, LocalDate dateDemande) {
        this.reservationId = reservationId;
        this.reservationNumero = reservationNumero;
        this.clientNom = clientNom;
        this.projetCode = projetCode;
        this.banque = banque;
        this.montantDemande = montantDemande;
        this.dateDemande = dateDemande;
    }

    public void accorder(BigDecimal montant, LocalDate date) {
        this.statut = Statut.ACCORDE;
        this.montantAccorde = montant;
        this.dateAccord = date;
    }

    public void refuser(LocalDate date) {
        this.statut = Statut.REFUSE;
        this.dateAccord = date;
    }

    public void debloquer(LocalDate date, Long encaissementId) {
        this.statut = Statut.DEBLOQUE;
        this.dateDeblocage = date;
        this.encaissementId = encaissementId;
    }

    public Long getId() { return id; }
    public Long getReservationId() { return reservationId; }
    public String getReservationNumero() { return reservationNumero; }
    public String getClientNom() { return clientNom; }
    public String getProjetCode() { return projetCode; }
    public String getBanque() { return banque; }
    public BigDecimal getMontantDemande() { return montantDemande; }
    public BigDecimal getMontantAccorde() { return montantAccorde; }
    public Statut getStatut() { return statut; }
    public LocalDate getDateDemande() { return dateDemande; }
    public LocalDate getDateAccord() { return dateAccord; }
    public LocalDate getDateDeblocage() { return dateDeblocage; }
    public Long getEncaissementId() { return encaissementId; }
}

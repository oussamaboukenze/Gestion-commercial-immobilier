package ma.ahi.gecimmo.postvente.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Notaire & Actes > Dossiers notaire. Un seul dossier par vente. */
@Entity
@Table(name = "dossiers_notaire")
public class DossierNotaire {

    public enum Statut { EN_PREPARATION, TRANSMIS, ACTE_SIGNE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false, unique = true)
    private Long reservationId;
    @Column(nullable = false, length = 20)
    private String reservationNumero;
    @Column(nullable = false)
    private Long lotId;
    @Column(nullable = false, length = 40)
    private String lotCode;
    @Column(nullable = false, length = 10)
    private String projetCode;
    @Column(nullable = false)
    private String clientNom;

    /** Notaire partenaire en charge de l'acte. */
    @Column(nullable = false)
    private String notaire;

    @Column(length = 2000)
    private String observations;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.EN_PREPARATION;

    private LocalDateTime dateOuverture;
    private LocalDate dateTransmission;
    private LocalDate dateSignatureActe;
    private String numeroActe;
    private String ouvertPar;

    protected DossierNotaire() {
    }

    public DossierNotaire(Long reservationId, String reservationNumero, Long lotId, String lotCode,
                          String projetCode, String clientNom, String notaire, String observations,
                          String ouvertPar) {
        this.reservationId = reservationId;
        this.reservationNumero = reservationNumero;
        this.lotId = lotId;
        this.lotCode = lotCode;
        this.projetCode = projetCode;
        this.clientNom = clientNom;
        this.notaire = notaire;
        this.observations = observations;
        this.ouvertPar = ouvertPar;
        this.dateOuverture = LocalDateTime.now();
    }

    public void transmettre(LocalDate date) {
        this.statut = Statut.TRANSMIS;
        this.dateTransmission = date;
    }

    public void signerActe(String numeroActe, LocalDate date) {
        this.statut = Statut.ACTE_SIGNE;
        this.numeroActe = numeroActe;
        this.dateSignatureActe = date;
    }

    public void setObservations(String observations) { this.observations = observations; }

    public Long getId() { return id; }
    public Long getReservationId() { return reservationId; }
    public String getReservationNumero() { return reservationNumero; }
    public Long getLotId() { return lotId; }
    public String getLotCode() { return lotCode; }
    public String getProjetCode() { return projetCode; }
    public String getClientNom() { return clientNom; }
    public String getNotaire() { return notaire; }
    public String getObservations() { return observations; }
    public Statut getStatut() { return statut; }
    public LocalDateTime getDateOuverture() { return dateOuverture; }
    public LocalDate getDateTransmission() { return dateTransmission; }
    public LocalDate getDateSignatureActe() { return dateSignatureActe; }
    public String getNumeroActe() { return numeroActe; }
    public String getOuvertPar() { return ouvertPar; }
}

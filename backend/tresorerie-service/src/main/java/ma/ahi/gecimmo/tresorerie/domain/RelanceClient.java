package ma.ahi.gecimmo.tresorerie.domain;

import java.math.BigDecimal;
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
import jakarta.persistence.UniqueConstraint;

/**
 * Relance d'un client en retard de paiement.
 * 1re relance J+7, 2e relance J+15, mise en demeure J+30 (validée par le DAF).
 */
@Entity
@Table(name = "relances_clients", uniqueConstraints = @UniqueConstraint(columnNames = {"echeance_id", "niveau"}))
public class RelanceClient {

    public enum Niveau {
        CHEQUE_IMPAYE(0), RELANCE_1(7), RELANCE_2(15), MISE_EN_DEMEURE(30);

        private final int joursRetard;

        Niveau(int joursRetard) {
            this.joursRetard = joursRetard;
        }

        public int joursRetard() {
            return joursRetard;
        }

        /** Niveau atteint selon le nombre de jours de retard (null si moins de 7 jours). */
        public static Niveau pourRetard(long jours) {
            if (jours >= MISE_EN_DEMEURE.joursRetard) {
                return MISE_EN_DEMEURE;
            }
            if (jours >= RELANCE_2.joursRetard) {
                return RELANCE_2;
            }
            if (jours >= RELANCE_1.joursRetard) {
                return RELANCE_1;
            }
            return null;
        }
    }

    public enum Statut { A_ENVOYER, EN_ATTENTE_VALIDATION_DAF, ENVOYEE, ANNULEE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "echeance_id")
    private Long echeanceId;

    @Column(nullable = false)
    private Long reservationId;
    @Column(nullable = false, length = 20)
    private String reservationNumero;
    @Column(nullable = false)
    private String clientNom;
    @Column(nullable = false, length = 10)
    private String projetCode;

    private String objet;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal montantDu;

    private LocalDate dateEcheance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Niveau niveau;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Statut statut;

    private LocalDateTime dateCreation;
    private LocalDateTime dateEnvoi;
    private String envoyeePar;
    private String valideeParDaf;

    protected RelanceClient() {
    }

    public RelanceClient(Long echeanceId, Long reservationId, String reservationNumero, String clientNom,
                         String projetCode, String objet, BigDecimal montantDu, LocalDate dateEcheance,
                         Niveau niveau) {
        this.echeanceId = echeanceId;
        this.reservationId = reservationId;
        this.reservationNumero = reservationNumero;
        this.clientNom = clientNom;
        this.projetCode = projetCode;
        this.objet = objet;
        this.montantDu = montantDu;
        this.dateEcheance = dateEcheance;
        this.niveau = niveau;
        this.statut = niveau == Niveau.MISE_EN_DEMEURE ? Statut.EN_ATTENTE_VALIDATION_DAF : Statut.A_ENVOYER;
        this.dateCreation = LocalDateTime.now();
    }

    public void validerParDaf(String daf) {
        this.valideeParDaf = daf;
        this.statut = Statut.A_ENVOYER;
    }

    public void marquerEnvoyee(String utilisateur) {
        this.statut = Statut.ENVOYEE;
        this.envoyeePar = utilisateur;
        this.dateEnvoi = LocalDateTime.now();
    }

    public void annuler() {
        this.statut = Statut.ANNULEE;
    }

    public Long getId() { return id; }
    public Long getEcheanceId() { return echeanceId; }
    public Long getReservationId() { return reservationId; }
    public String getReservationNumero() { return reservationNumero; }
    public String getClientNom() { return clientNom; }
    public String getProjetCode() { return projetCode; }
    public String getObjet() { return objet; }
    public BigDecimal getMontantDu() { return montantDu; }
    public LocalDate getDateEcheance() { return dateEcheance; }
    public Niveau getNiveau() { return niveau; }
    public Statut getStatut() { return statut; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public String getEnvoyeePar() { return envoyeePar; }
    public String getValideeParDaf() { return valideeParDaf; }
}

package ma.ahi.gecimmo.vente.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "echeances")
public class Echeance {

    public enum Etat { A_VENIR, PARTIELLE, PAYEE, EN_RETARD }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compromis_id")
    private Compromis compromis;

    private int ordre;

    @Column(nullable = false)
    private String libelle;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal pourcentage;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal montant;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal montantPaye = BigDecimal.ZERO;

    /** Date d'exigibilité : fixée par l'ADV (ex. constat d'achèvement du gros oeuvre). */
    private LocalDate dateEcheance;

    protected Echeance() {
    }

    Echeance(Compromis compromis, int ordre, String libelle, BigDecimal pourcentage, BigDecimal montant) {
        this.compromis = compromis;
        this.ordre = ordre;
        this.libelle = libelle;
        this.pourcentage = pourcentage;
        this.montant = montant;
    }

    public BigDecimal getResteAPayer() {
        return montant.subtract(montantPaye).max(BigDecimal.ZERO);
    }

    public Etat etat(LocalDate aujourdhui) {
        if (montantPaye.compareTo(montant) >= 0) {
            return Etat.PAYEE;
        }
        if (dateEcheance != null && dateEcheance.isBefore(aujourdhui)) {
            return Etat.EN_RETARD;
        }
        return montantPaye.signum() > 0 ? Etat.PARTIELLE : Etat.A_VENIR;
    }

    public Long getId() { return id; }
    public Compromis getCompromis() { return compromis; }
    public int getOrdre() { return ordre; }
    public String getLibelle() { return libelle; }
    public BigDecimal getPourcentage() { return pourcentage; }
    public BigDecimal getMontant() { return montant; }
    void setMontant(BigDecimal montant) { this.montant = montant; }
    public BigDecimal getMontantPaye() { return montantPaye; }
    void setMontantPaye(BigDecimal montantPaye) { this.montantPaye = montantPaye; }
    public LocalDate getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDate dateEcheance) { this.dateEcheance = dateEcheance; }
}

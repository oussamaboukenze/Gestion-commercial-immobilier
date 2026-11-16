package ma.ahi.gecimmo.referentiel.domain;

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

/** Matrice AHI : modification de grille de prix initiée par l'admin SI, validée par la direction. */
@Entity
@Table(name = "demandes_prix")
public class DemandePrix {

    public enum Statut { EN_ATTENTE, VALIDEE, REFUSEE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id")
    private Lot lot;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal ancienPrix;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal nouveauPrix;

    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.EN_ATTENTE;

    private String demandeur;
    private LocalDateTime dateDemande;
    private String validateur;
    private LocalDateTime dateDecision;

    protected DemandePrix() {
    }

    public DemandePrix(Lot lot, BigDecimal nouveauPrix, String motif, String demandeur) {
        this.lot = lot;
        this.ancienPrix = lot.getPrixTTC();
        this.nouveauPrix = nouveauPrix;
        this.motif = motif;
        this.demandeur = demandeur;
        this.dateDemande = LocalDateTime.now();
    }

    public void decider(boolean valide, String validateur) {
        this.statut = valide ? Statut.VALIDEE : Statut.REFUSEE;
        this.validateur = validateur;
        this.dateDecision = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Lot getLot() { return lot; }
    public BigDecimal getAncienPrix() { return ancienPrix; }
    public BigDecimal getNouveauPrix() { return nouveauPrix; }
    public String getMotif() { return motif; }
    public Statut getStatut() { return statut; }
    public String getDemandeur() { return demandeur; }
    public LocalDateTime getDateDemande() { return dateDemande; }
    public String getValidateur() { return validateur; }
    public LocalDateTime getDateDecision() { return dateDecision; }
}

package ma.ahi.gecimmo.tresorerie.domain;

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

/** Correction d'encaissement par contre-passation : initiée par le comptable, validée par le DAF. */
@Entity
@Table(name = "demandes_correction")
public class DemandeCorrection {

    public enum Statut { EN_ATTENTE_DAF, VALIDEE, REFUSEE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encaissement_id")
    private Encaissement encaissement;

    @Column(nullable = false, length = 1000)
    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.EN_ATTENTE_DAF;

    private String demandeur;
    private LocalDateTime dateDemande;
    private String validateurDaf;
    private LocalDateTime dateDecision;

    protected DemandeCorrection() {
    }

    public DemandeCorrection(Encaissement encaissement, String motif, String demandeur) {
        this.encaissement = encaissement;
        this.motif = motif;
        this.demandeur = demandeur;
        this.dateDemande = LocalDateTime.now();
    }

    public void decider(boolean valide, String daf) {
        this.statut = valide ? Statut.VALIDEE : Statut.REFUSEE;
        this.validateurDaf = daf;
        this.dateDecision = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Encaissement getEncaissement() { return encaissement; }
    public String getMotif() { return motif; }
    public Statut getStatut() { return statut; }
    public String getDemandeur() { return demandeur; }
    public LocalDateTime getDateDemande() { return dateDemande; }
    public String getValidateurDaf() { return validateurDaf; }
    public LocalDateTime getDateDecision() { return dateDecision; }
}

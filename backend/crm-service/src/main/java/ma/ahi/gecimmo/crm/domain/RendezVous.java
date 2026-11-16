package ma.ahi.gecimmo.crm.domain;

import java.time.Duration;
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

@Entity
@Table(name = "rendez_vous")
public class RendezVous {

    public enum Type { VISITE, RDV_BUREAU, APPEL }

    public enum Statut { PLANIFIE, EFFECTUE, ANNULE }

    /** Règle AHI : compte rendu obligatoire sous 24 h. */
    public static final Duration DELAI_COMPTE_RENDU = Duration.ofHours(24);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prospect_id")
    private Prospect prospect;

    @Column(nullable = false)
    private LocalDateTime dateHeure;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    /** Lot visité (optionnel), référence vers referentiel-service. */
    private Long lotId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.PLANIFIE;

    @Column(length = 2000)
    private String compteRendu;

    private LocalDateTime dateCompteRendu;

    private boolean compteRenduHorsDelai;

    @Column(nullable = false)
    private String commercial;

    protected RendezVous() {
    }

    public RendezVous(Prospect prospect, LocalDateTime dateHeure, Type type, Long lotId, String commercial) {
        this.prospect = prospect;
        this.dateHeure = dateHeure;
        this.type = type;
        this.lotId = lotId;
        this.commercial = commercial;
    }

    public void saisirCompteRendu(String texte, LocalDateTime maintenant) {
        this.compteRendu = texte;
        this.dateCompteRendu = maintenant;
        this.statut = Statut.EFFECTUE;
        this.compteRenduHorsDelai = maintenant.isAfter(dateHeure.plus(DELAI_COMPTE_RENDU));
    }

    public void annuler() {
        this.statut = Statut.ANNULE;
    }

    public Long getId() { return id; }
    public Prospect getProspect() { return prospect; }
    public LocalDateTime getDateHeure() { return dateHeure; }
    public Type getType() { return type; }
    public Long getLotId() { return lotId; }
    public Statut getStatut() { return statut; }
    public String getCompteRendu() { return compteRendu; }
    public LocalDateTime getDateCompteRendu() { return dateCompteRendu; }
    public boolean isCompteRenduHorsDelai() { return compteRenduHorsDelai; }
    public String getCommercial() { return commercial; }
}

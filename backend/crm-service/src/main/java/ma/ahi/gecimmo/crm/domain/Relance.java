package ma.ahi.gecimmo.crm.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Relance commerciale d'un prospect (CRM > Relances). */
@Entity
@Table(name = "relances_crm")
public class Relance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prospect_id")
    private Prospect prospect;

    @Column(nullable = false)
    private LocalDate dateRelance;

    @Column(nullable = false)
    private String objet;

    private boolean faite;

    private LocalDateTime dateFaite;

    @Column(length = 1000)
    private String resultat;

    @Column(nullable = false)
    private String commercial;

    protected Relance() {
    }

    public Relance(Prospect prospect, LocalDate dateRelance, String objet, String commercial) {
        this.prospect = prospect;
        this.dateRelance = dateRelance;
        this.objet = objet;
        this.commercial = commercial;
    }

    public void marquerFaite(String resultat) {
        this.faite = true;
        this.resultat = resultat;
        this.dateFaite = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Prospect getProspect() { return prospect; }
    public LocalDate getDateRelance() { return dateRelance; }
    public String getObjet() { return objet; }
    public boolean isFaite() { return faite; }
    public LocalDateTime getDateFaite() { return dateFaite; }
    public String getResultat() { return resultat; }
    public String getCommercial() { return commercial; }
    public void setCommercial(String commercial) { this.commercial = commercial; }
}

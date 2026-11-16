package ma.ahi.gecimmo.crm.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "prospects", indexes = {
        @Index(name = "idx_prospect_commercial", columnList = "commercial"),
        @Index(name = "idx_prospect_cin", columnList = "cin")})
public class Prospect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    private String prenom;

    @Column(nullable = false, length = 20)
    private String telephone;

    @Column(nullable = false, length = 20)
    private String cin;

    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceProspect source;

    @Column(precision = 14, scale = 2)
    private BigDecimal budget;

    /** Typologie recherchée : STUDIO, F2, F3, VILLA... */
    @Column(length = 20)
    private String typologieRecherchee;

    /** Projet visé (RNA, JBK...) : sert au contrôle d'accès par projet. */
    @Column(nullable = false, length = 10)
    private String projetCode;

    /** Username Keycloak du commercial propriétaire. */
    @Column(nullable = false)
    private String commercial;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProspectStatut statut = ProspectStatut.NOUVEAU;

    private String motifPerte;

    private LocalDateTime dateCreation;

    /** Date à laquelle la réservation a été validée par l'ADV. */
    private LocalDateTime dateConversion;

    protected Prospect() {
    }

    public Prospect(String nom, String prenom, String telephone, String cin, String email, SourceProspect source,
                    BigDecimal budget, String typologieRecherchee, String projetCode, String commercial) {
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.cin = cin.toUpperCase();
        this.email = email;
        this.source = source;
        this.budget = budget;
        this.typologieRecherchee = typologieRecherchee;
        this.projetCode = projetCode.toUpperCase();
        this.commercial = commercial;
        this.dateCreation = LocalDateTime.now();
    }

    public void convertir() {
        this.statut = ProspectStatut.CONVERTI;
        this.dateConversion = LocalDateTime.now();
    }

    public String nomComplet() {
        return prenom == null || prenom.isBlank() ? nom : prenom + " " + nom;
    }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getCin() { return cin; }
    public void setCin(String cin) { this.cin = cin.toUpperCase(); }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public SourceProspect getSource() { return source; }
    public void setSource(SourceProspect source) { this.source = source; }
    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }
    public String getTypologieRecherchee() { return typologieRecherchee; }
    public void setTypologieRecherchee(String typologieRecherchee) { this.typologieRecherchee = typologieRecherchee; }
    public String getProjetCode() { return projetCode; }
    public String getCommercial() { return commercial; }
    public void setCommercial(String commercial) { this.commercial = commercial; }
    public ProspectStatut getStatut() { return statut; }
    public void setStatut(ProspectStatut statut) { this.statut = statut; }
    public String getMotifPerte() { return motifPerte; }
    public void setMotifPerte(String motifPerte) { this.motifPerte = motifPerte; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public LocalDateTime getDateConversion() { return dateConversion; }
}

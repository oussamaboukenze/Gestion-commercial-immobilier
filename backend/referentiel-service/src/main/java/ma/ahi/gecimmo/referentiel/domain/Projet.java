package ma.ahi.gecimmo.referentiel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "projets")
public class Projet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Code court du projet : RNA, JBK, OCE, SAL. */
    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String ville;

    /** Ex. "Moyen standing", "Logement social", "Bureaux + commerces". */
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjetStatut statut;

    protected Projet() {
    }

    public Projet(String code, String nom, String ville, String type, ProjetStatut statut) {
        this.code = code.toUpperCase();
        this.nom = nom;
        this.ville = ville;
        this.type = type;
        this.statut = statut;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public ProjetStatut getStatut() { return statut; }
    public void setStatut(ProjetStatut statut) { this.statut = statut; }
}

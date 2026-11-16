package ma.ahi.gecimmo.referentiel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "tranches", uniqueConstraints = @UniqueConstraint(columnNames = {"projet_id", "code"}))
public class Tranche {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ex. T1 */
    @Column(nullable = false, length = 10)
    private String code;

    private String nom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projet_id")
    private Projet projet;

    protected Tranche() {
    }

    public Tranche(String code, String nom, Projet projet) {
        this.code = code.toUpperCase();
        this.nom = nom;
        this.projet = projet;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getNom() { return nom; }
    public Projet getProjet() { return projet; }
}

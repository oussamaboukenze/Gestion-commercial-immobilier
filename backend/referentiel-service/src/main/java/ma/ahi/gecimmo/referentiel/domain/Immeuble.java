package ma.ahi.gecimmo.referentiel.domain;

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
import jakarta.persistence.UniqueConstraint;

/** Immeuble (résidentiel) ou îlot (villas). */
@Entity
@Table(name = "immeubles", uniqueConstraints = @UniqueConstraint(columnNames = {"tranche_id", "code"}))
public class Immeuble {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ex. IMM03 */
    @Column(nullable = false, length = 10)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeBatiment type;

    private Integer nombreEtages;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tranche_id")
    private Tranche tranche;

    protected Immeuble() {
    }

    public Immeuble(String code, TypeBatiment type, Integer nombreEtages, Tranche tranche) {
        this.code = code.toUpperCase();
        this.type = type;
        this.nombreEtages = nombreEtages;
        this.tranche = tranche;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public TypeBatiment getType() { return type; }
    public Integer getNombreEtages() { return nombreEtages; }
    public Tranche getTranche() { return tranche; }
}

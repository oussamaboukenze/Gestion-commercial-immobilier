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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import ma.ahi.gecimmo.referentiel.exception.BusinessException;

@Entity
@Table(name = "lots", indexes = {
        @Index(name = "idx_lot_projet_statut", columnList = "projet_code,statut"),
        @Index(name = "idx_lot_typologie", columnList = "typologie")})
public class Lot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Code complet : RNA-T1-IMM03-E2-A12 */
    @Column(nullable = false, unique = true, length = 40)
    private String code;

    /** Numéro du lot dans l'étage : A12 */
    @Column(nullable = false, length = 10)
    private String numero;

    private int etage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Typologie typologie;

    /** Surface en m² */
    @Column(precision = 10, scale = 2)
    private BigDecimal surface;

    /** Prix TTC en dirhams (grille de prix validée par la direction). */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal prixTTC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LotStatut statut = LotStatut.DISPONIBLE;

    private LocalDateTime dateMajStatut;

    /** Copie du code projet pour filtrer vite (et contrôler l'accès par projet). */
    @Column(name = "projet_code", nullable = false, length = 10)
    private String projetCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "immeuble_id")
    private Immeuble immeuble;

    /** Verrou optimiste : deux changements de statut simultanés -> l'un échoue (pas de double vente). */
    @Version
    private Long version;

    protected Lot() {
    }

    public Lot(Immeuble immeuble, String numero, int etage, Typologie typologie,
               BigDecimal surface, BigDecimal prixTTC) {
        Tranche tranche = immeuble.getTranche();
        Projet projet = tranche.getProjet();
        this.immeuble = immeuble;
        this.numero = numero.toUpperCase();
        this.etage = etage;
        this.typologie = typologie;
        this.surface = surface;
        this.prixTTC = prixTTC;
        this.projetCode = projet.getCode();
        this.code = String.join("-", projet.getCode(), tranche.getCode(), immeuble.getCode(),
                "E" + etage, this.numero);
        this.dateMajStatut = LocalDateTime.now();
    }

    /** Change le statut en respectant les transitions autorisées. */
    public void changerStatut(LotStatut cible) {
        if (!statut.peutPasserA(cible)) {
            throw BusinessException.conflict(
                    "Le lot " + code + " est " + statut + " : passage à " + cible + " impossible.");
        }
        this.statut = cible;
        this.dateMajStatut = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getNumero() { return numero; }
    public int getEtage() { return etage; }
    public Typologie getTypologie() { return typologie; }
    public void setTypologie(Typologie typologie) { this.typologie = typologie; }
    public BigDecimal getSurface() { return surface; }
    public void setSurface(BigDecimal surface) { this.surface = surface; }
    public BigDecimal getPrixTTC() { return prixTTC; }
    public void setPrixTTC(BigDecimal prixTTC) { this.prixTTC = prixTTC; }
    public LotStatut getStatut() { return statut; }
    public LocalDateTime getDateMajStatut() { return dateMajStatut; }
    public String getProjetCode() { return projetCode; }
    public Immeuble getImmeuble() { return immeuble; }
    public Long getVersion() { return version; }
}

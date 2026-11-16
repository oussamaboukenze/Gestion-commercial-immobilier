package ma.ahi.gecimmo.tresorerie.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Chèque rejeté par la banque. */
@Entity
@Table(name = "impayes")
public class Impaye {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encaissement_id", unique = true)
    private Encaissement encaissement;

    @Column(nullable = false)
    private String motif;

    @Column(nullable = false)
    private LocalDate dateRejet;

    private String saisiPar;
    private LocalDateTime dateSaisie;

    protected Impaye() {
    }

    public Impaye(Encaissement encaissement, String motif, LocalDate dateRejet, String saisiPar) {
        this.encaissement = encaissement;
        this.motif = motif;
        this.dateRejet = dateRejet;
        this.saisiPar = saisiPar;
        this.dateSaisie = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Encaissement getEncaissement() { return encaissement; }
    public String getMotif() { return motif; }
    public LocalDate getDateRejet() { return dateRejet; }
    public String getSaisiPar() { return saisiPar; }
    public LocalDateTime getDateSaisie() { return dateSaisie; }
}

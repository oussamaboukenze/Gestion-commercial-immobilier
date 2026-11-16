package ma.ahi.gecimmo.tresorerie.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/** Bordereau de remise de chèques en banque. */
@Entity
@Table(name = "remises_banque")
public class RemiseEnBanque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 20)
    private String numero;

    @Column(nullable = false)
    private String banque;

    @Column(nullable = false)
    private LocalDate dateRemise;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal montantTotal = BigDecimal.ZERO;

    private String saisiPar;

    @OneToMany(mappedBy = "remise")
    private List<Encaissement> cheques = new ArrayList<>();

    protected RemiseEnBanque() {
    }

    public RemiseEnBanque(String banque, LocalDate dateRemise, String saisiPar) {
        this.banque = banque;
        this.dateRemise = dateRemise;
        this.saisiPar = saisiPar;
    }

    public void ajouter(Encaissement cheque) {
        cheques.add(cheque);
        cheque.affecterRemise(this);
        montantTotal = montantTotal.add(cheque.getMontant());
    }

    public void attribuerNumero() {
        this.numero = String.format("RB-%d-%05d", dateRemise.getYear(), id);
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public String getBanque() { return banque; }
    public LocalDate getDateRemise() { return dateRemise; }
    public BigDecimal getMontantTotal() { return montantTotal; }
    public String getSaisiPar() { return saisiPar; }
    public List<Encaissement> getCheques() { return cheques; }
}

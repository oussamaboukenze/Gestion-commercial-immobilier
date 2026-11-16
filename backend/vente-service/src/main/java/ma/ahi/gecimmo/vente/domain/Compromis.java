package ma.ahi.gecimmo.vente.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/** Compromis de vente + échéancier (20 % / 30 % / 50 %). */
@Entity
@Table(name = "compromis")
public class Compromis {

    public enum Statut { BROUILLON, SIGNE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", unique = true)
    private Reservation reservation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.BROUILLON;

    @Column(nullable = false)
    private LocalDateTime dateCreation;
    private LocalDateTime dateSignature;
    private String etabliPar;

    @OneToMany(mappedBy = "compromis", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre")
    private List<Echeance> echeances = new ArrayList<>();

    protected Compromis() {
    }

    public Compromis(Reservation reservation, String etabliPar) {
        this.reservation = reservation;
        this.etabliPar = etabliPar;
        this.dateCreation = LocalDateTime.now();
        List<BigDecimal> montants = RegleVente.montantsEcheancier(reservation.getPrixNet());
        for (int i = 0; i < RegleVente.ECHEANCIER_STANDARD.size(); i++) {
            RegleVente.EcheanceModele modele = RegleVente.ECHEANCIER_STANDARD.get(i);
            echeances.add(new Echeance(this, i + 1, modele.libelle(), modele.pourcentage(), montants.get(i)));
        }
        repartirEncaissements();
    }

    /** Recalcule le "payé" de chaque échéance à partir du total encaissé du dossier. */
    public void repartirEncaissements() {
        List<BigDecimal> payes = RegleVente.repartir(reservation.getTotalEncaisse(),
                echeances.stream().map(Echeance::getMontant).toList());
        for (int i = 0; i < echeances.size(); i++) {
            echeances.get(i).setMontantPaye(payes.get(i));
        }
    }

    /** Changement de lot : nouveaux montants selon le nouveau prix net. */
    public void recalculerMontants() {
        List<BigDecimal> montants = RegleVente.montantsEcheancier(reservation.getPrixNet());
        for (int i = 0; i < echeances.size(); i++) {
            echeances.get(i).setMontant(montants.get(i));
        }
        repartirEncaissements();
    }

    public void signer(LocalDateTime maintenant) {
        this.statut = Statut.SIGNE;
        this.dateSignature = maintenant;
    }

    public Long getId() { return id; }
    public Reservation getReservation() { return reservation; }
    public Statut getStatut() { return statut; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public LocalDateTime getDateSignature() { return dateSignature; }
    public String getEtabliPar() { return etabliPar; }
    public List<Echeance> getEcheances() { return echeances; }
}

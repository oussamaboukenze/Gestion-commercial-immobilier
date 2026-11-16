package ma.ahi.gecimmo.vente.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Option : blocage d'un lot 48 h maximum pour un prospect (1 option active par prospect). */
@Entity
@Table(name = "options_lot")
public class OptionLot {

    public enum Statut { ACTIVE, EXPIREE, LEVEE, TRANSFORMEE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long lotId;

    @Column(nullable = false, length = 40)
    private String lotCode;

    @Column(nullable = false, length = 10)
    private String projetCode;

    @Column(nullable = false)
    private Long prospectId;

    @Column(nullable = false)
    private String commercial;

    @Column(nullable = false)
    private LocalDateTime dateDebut;

    @Column(nullable = false)
    private LocalDateTime dateExpiration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.ACTIVE;

    protected OptionLot() {
    }

    public OptionLot(Long lotId, String lotCode, String projetCode, Long prospectId, String commercial,
                     LocalDateTime maintenant) {
        this.lotId = lotId;
        this.lotCode = lotCode;
        this.projetCode = projetCode;
        this.prospectId = prospectId;
        this.commercial = commercial;
        this.dateDebut = maintenant;
        this.dateExpiration = maintenant.plusHours(RegleVente.DUREE_OPTION_HEURES);
    }

    public boolean estActive(LocalDateTime maintenant) {
        return statut == Statut.ACTIVE && maintenant.isBefore(dateExpiration);
    }

    public void changerStatut(Statut statut) {
        this.statut = statut;
    }

    public Long getId() { return id; }
    public Long getLotId() { return lotId; }
    public String getLotCode() { return lotCode; }
    public String getProjetCode() { return projetCode; }
    public Long getProspectId() { return prospectId; }
    public String getCommercial() { return commercial; }
    public LocalDateTime getDateDebut() { return dateDebut; }
    public LocalDateTime getDateExpiration() { return dateExpiration; }
    public Statut getStatut() { return statut; }
}

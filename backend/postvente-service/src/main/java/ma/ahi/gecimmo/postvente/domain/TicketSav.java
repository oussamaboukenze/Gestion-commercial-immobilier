package ma.ahi.gecimmo.postvente.domain;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/** SAV > Tickets : réclamation d'un client livré (12 mois max). */
@Entity
@Table(name = "tickets_sav")
public class TicketSav {

    public enum Categorie { PLOMBERIE, ELECTRICITE, MENUISERIE, PEINTURE, ETANCHEITE, CARRELAGE, AUTRE }

    public enum Priorite { BASSE, NORMALE, HAUTE }

    public enum Statut { OUVERT, EN_COURS, RESOLU, CLOS }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 20)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "livraison_id")
    private Livraison livraison;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Categorie categorie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priorite priorite;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.OUVERT;

    private LocalDateTime dateOuverture;
    private String ouvertPar;
    private LocalDateTime dateCloture;
    @Column(length = 1000)
    private String commentaireCloture;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dateIntervention")
    private List<Intervention> interventions = new ArrayList<>();

    protected TicketSav() {
    }

    public TicketSav(Livraison livraison, Categorie categorie, Priorite priorite, String description, String ouvertPar) {
        this.livraison = livraison;
        this.categorie = categorie;
        this.priorite = priorite;
        this.description = description;
        this.ouvertPar = ouvertPar;
        this.dateOuverture = LocalDateTime.now();
    }

    public void attribuerNumero() {
        this.numero = String.format("SAV-%d-%05d", dateOuverture.getYear(), id);
    }

    public Intervention ajouterIntervention(LocalDateTime date, String intervenant, String travaux) {
        Intervention i = new Intervention(this, date, intervenant, travaux);
        interventions.add(i);
        if (statut == Statut.OUVERT) {
            statut = Statut.EN_COURS;
        }
        return i;
    }

    public void resoudre() {
        this.statut = Statut.RESOLU;
    }

    public void cloturer(String commentaire) {
        this.statut = Statut.CLOS;
        this.commentaireCloture = commentaire;
        this.dateCloture = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public Livraison getLivraison() { return livraison; }
    public Categorie getCategorie() { return categorie; }
    public Priorite getPriorite() { return priorite; }
    public String getDescription() { return description; }
    public Statut getStatut() { return statut; }
    public LocalDateTime getDateOuverture() { return dateOuverture; }
    public String getOuvertPar() { return ouvertPar; }
    public LocalDateTime getDateCloture() { return dateCloture; }
    public String getCommentaireCloture() { return commentaireCloture; }
    public List<Intervention> getInterventions() { return interventions; }
}

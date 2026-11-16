package ma.ahi.gecimmo.postvente.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Livraison : planning > pré-réception > PV de livraison + remise des clés (solde = 0). */
@Entity
@Table(name = "livraisons")
public class Livraison {

    public enum Statut { PLANIFIEE, PRE_RECEPTION_FAITE, LIVREE, ANNULEE }

    /** Les réclamations SAV sont ouvertes jusqu'à 12 mois après la livraison. */
    public static final int DUREE_SAV_MOIS = 12;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false, unique = true)
    private Long reservationId;
    @Column(nullable = false, length = 20)
    private String reservationNumero;
    @Column(nullable = false)
    private Long lotId;
    @Column(nullable = false, length = 40)
    private String lotCode;
    @Column(nullable = false, length = 10)
    private String projetCode;
    @Column(nullable = false)
    private String clientNom;
    private String clientTelephone;

    @Column(nullable = false)
    private LocalDate datePrevue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Statut statut = Statut.PLANIFIEE;

    private LocalDate datePreReception;
    /** Réserves notées lors de la pré-réception (finitions à reprendre). */
    @Column(length = 4000)
    private String reserves;
    private boolean reservesLevees;

    private LocalDate dateLivraison;
    private String livrePar;
    private boolean clesRemises;
    private int nombreCles;

    protected Livraison() {
    }

    public Livraison(Long reservationId, String reservationNumero, Long lotId, String lotCode, String projetCode,
                     String clientNom, String clientTelephone, LocalDate datePrevue) {
        this.reservationId = reservationId;
        this.reservationNumero = reservationNumero;
        this.lotId = lotId;
        this.lotCode = lotCode;
        this.projetCode = projetCode;
        this.clientNom = clientNom;
        this.clientTelephone = clientTelephone;
        this.datePrevue = datePrevue;
    }

    public void replanifier(LocalDate date) {
        this.datePrevue = date;
    }

    public void preReception(LocalDate date, String reserves) {
        this.datePreReception = date;
        this.reserves = reserves;
        this.reservesLevees = reserves == null || reserves.isBlank();
        this.statut = Statut.PRE_RECEPTION_FAITE;
    }

    public void leverReserves() {
        this.reservesLevees = true;
    }

    public void livrer(LocalDate date, int nombreCles, String utilisateur) {
        this.dateLivraison = date;
        this.nombreCles = nombreCles;
        this.clesRemises = true;
        this.livrePar = utilisateur;
        this.statut = Statut.LIVREE;
    }

    public void annuler() {
        this.statut = Statut.ANNULEE;
    }

    public boolean savOuvert(LocalDate aujourdhui) {
        return statut == Statut.LIVREE && !aujourdhui.isAfter(dateLivraison.plusMonths(DUREE_SAV_MOIS));
    }

    public Long getId() { return id; }
    public Long getReservationId() { return reservationId; }
    public String getReservationNumero() { return reservationNumero; }
    public Long getLotId() { return lotId; }
    public String getLotCode() { return lotCode; }
    public String getProjetCode() { return projetCode; }
    public String getClientNom() { return clientNom; }
    public String getClientTelephone() { return clientTelephone; }
    public LocalDate getDatePrevue() { return datePrevue; }
    public Statut getStatut() { return statut; }
    public LocalDate getDatePreReception() { return datePreReception; }
    public String getReserves() { return reserves; }
    public boolean isReservesLevees() { return reservesLevees; }
    public LocalDate getDateLivraison() { return dateLivraison; }
    public String getLivrePar() { return livrePar; }
    public boolean isClesRemises() { return clesRemises; }
    public int getNombreCles() { return nombreCles; }
}

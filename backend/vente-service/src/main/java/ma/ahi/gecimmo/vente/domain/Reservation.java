package ma.ahi.gecimmo.vente.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Dossier de vente : naît à la réservation et suit le client jusqu'au compromis.
 * Les versements (encaissés par la trésorerie) sont rattachés à la réservation.
 */
@Entity
@Table(name = "reservations", indexes = {
        @Index(name = "idx_resa_lot", columnList = "lot_id"),
        @Index(name = "idx_resa_commercial", columnList = "commercial"),
        @Index(name = "idx_resa_statut", columnList = "statut")})
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ex. RES-2026-000042 */
    @Column(unique = true, length = 20)
    private String numero;

    // ---- Lot (référence vers referentiel-service) ----
    @Column(name = "lot_id", nullable = false)
    private Long lotId;
    @Column(nullable = false, length = 40)
    private String lotCode;
    @Column(nullable = false, length = 10)
    private String projetCode;

    // ---- Client (référence vers crm-service) ----
    @Column(nullable = false)
    private Long prospectId;
    @Column(nullable = false)
    private String clientNom;
    @Column(nullable = false, length = 20)
    private String clientCin;
    private String clientTelephone;

    @Column(nullable = false)
    private String commercial;

    // ---- Prix ----
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal prixTTC;
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal remisePct = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal prixNet;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutRemise statutRemise;
    private String validateurRemise;

    // ---- Avance & versements ----
    /** Montant d'avance déclaré (>= 5 % du prix net). */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal avance;
    /** Total réellement encaissé et confirmé par la trésorerie. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalEncaisse = BigDecimal.ZERO;

    // ---- Pièces du dossier ----
    private boolean pieceCin;
    private boolean pieceJustificatifRevenus;
    private boolean pieceRecuAvance;

    // ---- Statut & traçabilité ----
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReservationStatut statut = ReservationStatut.EN_ATTENTE_VALIDATION_ADV;
    @Column(nullable = false)
    private LocalDateTime dateCreation;
    private LocalDateTime dateValidation;
    private String validateurAdv;
    /** Réservation valable 30 jours : au-delà, compromis obligatoire. */
    private LocalDate dateLimiteCompromis;
    private String motifRejet;

    @Version
    private Long version;

    protected Reservation() {
    }

    public Reservation(Long lotId, String lotCode, String projetCode, Long prospectId, String clientNom,
                       String clientCin, String clientTelephone, String commercial, BigDecimal prixTTC,
                       BigDecimal remisePct, BigDecimal avance) {
        this.lotId = lotId;
        this.lotCode = lotCode;
        this.projetCode = projetCode;
        this.prospectId = prospectId;
        this.clientNom = clientNom;
        this.clientCin = clientCin;
        this.clientTelephone = clientTelephone;
        this.commercial = commercial;
        this.prixTTC = prixTTC;
        this.remisePct = remisePct == null ? BigDecimal.ZERO : remisePct;
        this.prixNet = RegleVente.prixNet(prixTTC, this.remisePct);
        this.statutRemise = RegleVente.statutRemiseInitial(this.remisePct);
        this.avance = avance;
        this.dateCreation = LocalDateTime.now();
    }

    public void attribuerNumero() {
        this.numero = String.format("RES-%d-%06d", dateCreation.getYear(), id);
    }

    public boolean dossierComplet() {
        return pieceCin && pieceJustificatifRevenus && pieceRecuAvance;
    }

    public boolean avanceConfirmee() {
        return totalEncaisse.compareTo(avance) >= 0;
    }

    public boolean estActive() {
        return ReservationStatut.ACTIFS.contains(statut);
    }

    public void valider(String adv, LocalDateTime maintenant) {
        this.statut = ReservationStatut.VALIDEE;
        this.validateurAdv = adv;
        this.dateValidation = maintenant;
        this.dateLimiteCompromis = maintenant.toLocalDate().plusDays(RegleVente.VALIDITE_RESERVATION_JOURS);
    }

    public void rejeter(String motif) {
        this.statut = ReservationStatut.REJETEE;
        this.motifRejet = motif;
    }

    public void deciderRemise(boolean accordee, String validateur) {
        this.statutRemise = accordee ? StatutRemise.VALIDEE : StatutRemise.REFUSEE;
        this.validateurRemise = validateur;
    }

    /** Changement de lot : les versements restent sur le dossier, le prix suit le nouveau lot. */
    public void changerLot(Long lotId, String lotCode, BigDecimal prixTTC) {
        this.lotId = lotId;
        this.lotCode = lotCode;
        this.prixTTC = prixTTC;
        this.prixNet = RegleVente.prixNet(prixTTC, remisePct);
    }

    public void ajouterEncaissement(BigDecimal montant) {
        this.totalEncaisse = RegleVente.arrondir(totalEncaisse.add(montant));
    }

    public void setPieces(boolean cin, boolean justificatifRevenus, boolean recuAvance) {
        this.pieceCin = cin;
        this.pieceJustificatifRevenus = justificatifRevenus;
        this.pieceRecuAvance = recuAvance;
    }

    public void setStatut(ReservationStatut statut) { this.statut = statut; }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public Long getLotId() { return lotId; }
    public String getLotCode() { return lotCode; }
    public String getProjetCode() { return projetCode; }
    public Long getProspectId() { return prospectId; }
    public String getClientNom() { return clientNom; }
    public String getClientCin() { return clientCin; }
    public String getClientTelephone() { return clientTelephone; }
    public String getCommercial() { return commercial; }
    public BigDecimal getPrixTTC() { return prixTTC; }
    public BigDecimal getRemisePct() { return remisePct; }
    public BigDecimal getPrixNet() { return prixNet; }
    public StatutRemise getStatutRemise() { return statutRemise; }
    public String getValidateurRemise() { return validateurRemise; }
    public BigDecimal getAvance() { return avance; }
    public BigDecimal getTotalEncaisse() { return totalEncaisse; }
    public boolean isPieceCin() { return pieceCin; }
    public boolean isPieceJustificatifRevenus() { return pieceJustificatifRevenus; }
    public boolean isPieceRecuAvance() { return pieceRecuAvance; }
    public ReservationStatut getStatut() { return statut; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public LocalDateTime getDateValidation() { return dateValidation; }
    public String getValidateurAdv() { return validateurAdv; }
    public LocalDate getDateLimiteCompromis() { return dateLimiteCompromis; }
    public String getMotifRejet() { return motifRejet; }
}

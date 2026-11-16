package ma.ahi.gecimmo.vente.domain;

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

/** Pièce du dossier (CIN, justificatif de revenus, reçu de l'avance...) stockée sur disque. */
@Entity
@Table(name = "pieces_jointes", indexes = @Index(name = "idx_piece_resa", columnList = "reservation_id"))
public class PieceJointe {

    public enum Type { CIN, JUSTIFICATIF_REVENUS, RECU_AVANCE, AUTRE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Type type;

    @Column(nullable = false)
    private String nomFichier;

    private String contentType;
    private long taille;

    /** Chemin relatif dans le dossier de stockage. */
    @Column(nullable = false)
    private String chemin;

    private LocalDateTime dateDepot;
    private String deposePar;

    protected PieceJointe() {
    }

    public PieceJointe(Long reservationId, Type type, String nomFichier, String contentType, long taille,
                       String chemin, String deposePar) {
        this.reservationId = reservationId;
        this.type = type;
        this.nomFichier = nomFichier;
        this.contentType = contentType;
        this.taille = taille;
        this.chemin = chemin;
        this.deposePar = deposePar;
        this.dateDepot = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getReservationId() { return reservationId; }
    public Type getType() { return type; }
    public String getNomFichier() { return nomFichier; }
    public String getContentType() { return contentType; }
    public long getTaille() { return taille; }
    public String getChemin() { return chemin; }
    public LocalDateTime getDateDepot() { return dateDepot; }
    public String getDeposePar() { return deposePar; }
}

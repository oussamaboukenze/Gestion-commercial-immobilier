package ma.ahi.gecimmo.vente.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Trace des événements reçus de la trésorerie.
 * La contrainte unique (encaissement, type) rend le traitement idempotent :
 * un message RabbitMQ reçu deux fois n'est compté qu'une seule fois.
 */
@Entity
@Table(name = "encaissements_recus",
        uniqueConstraints = @UniqueConstraint(columnNames = {"encaissement_id", "type"}))
public class EncaissementRecu {

    public enum Type { ENREGISTRE, ANNULE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "encaissement_id", nullable = false)
    private Long encaissementId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false)
    private Long reservationId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal montant;

    private LocalDateTime dateReception;

    protected EncaissementRecu() {
    }

    public EncaissementRecu(Long encaissementId, Type type, Long reservationId, BigDecimal montant) {
        this.encaissementId = encaissementId;
        this.type = type;
        this.reservationId = reservationId;
        this.montant = montant;
        this.dateReception = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getEncaissementId() { return encaissementId; }
    public Type getType() { return type; }
    public Long getReservationId() { return reservationId; }
    public BigDecimal getMontant() { return montant; }
}

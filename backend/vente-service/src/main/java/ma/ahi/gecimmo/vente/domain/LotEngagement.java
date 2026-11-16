package ma.ahi.gecimmo.vente.domain;

import java.time.LocalDateTime;

import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Verrou en base : un lot ne peut avoir qu'UNE réservation active.
 * La clé primaire est l'id du lot : une 2e insertion pour le même lot échoue
 * (même si deux commerciaux cliquent au même moment).
 */
@Entity
@Table(name = "lots_engages")
public class LotEngagement implements Persistable<Long> {

    @Id
    private Long lotId;

    @Column(nullable = false)
    private Long reservationId;

    @Column(nullable = false)
    private LocalDateTime dateEngagement;

    @Transient
    private boolean nouveau = true;

    protected LotEngagement() {
        this.nouveau = false;
    }

    public LotEngagement(Long lotId, Long reservationId) {
        this.lotId = lotId;
        this.reservationId = reservationId;
        this.dateEngagement = LocalDateTime.now();
    }

    /** Toujours "nouveau" à la création : save() fait un INSERT (jamais un UPDATE silencieux). */
    @Override
    public boolean isNew() {
        return nouveau;
    }

    @Override
    public Long getId() {
        return lotId;
    }

    public Long getLotId() { return lotId; }
    public Long getReservationId() { return reservationId; }
    public LocalDateTime getDateEngagement() { return dateEngagement; }
}

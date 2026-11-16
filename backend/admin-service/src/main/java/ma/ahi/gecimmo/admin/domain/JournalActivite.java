package ma.ahi.gecimmo.admin.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** Administration > Journal d'activité : trace de chaque événement métier de GECIMMO. */
@Entity
@Table(name = "journal_activite", indexes = @Index(name = "idx_journal_date", columnList = "date_evenement"))
public class JournalActivite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_evenement", nullable = false)
    private LocalDateTime date;

    /** Clé de routage RabbitMQ : reservation.validee, encaissement.enregistre, admin.utilisateur... */
    @Column(nullable = false, length = 60)
    private String evenement;

    @Column(nullable = false, length = 4000)
    private String details;

    protected JournalActivite() {
    }

    public JournalActivite(String evenement, String details) {
        this.date = LocalDateTime.now();
        this.evenement = evenement;
        this.details = details.length() > 4000 ? details.substring(0, 4000) : details;
    }

    public Long getId() { return id; }
    public LocalDateTime getDate() { return date; }
    public String getEvenement() { return evenement; }
    public String getDetails() { return details; }
}

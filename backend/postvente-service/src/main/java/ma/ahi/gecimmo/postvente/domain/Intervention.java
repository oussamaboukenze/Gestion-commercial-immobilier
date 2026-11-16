package ma.ahi.gecimmo.postvente.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** SAV > Interventions : travaux réalisés sur un ticket. */
@Entity
@Table(name = "interventions_sav")
public class Intervention {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id")
    private TicketSav ticket;

    @Column(nullable = false)
    private LocalDateTime dateIntervention;

    @Column(nullable = false)
    private String intervenant;

    @Column(nullable = false, length = 2000)
    private String travaux;

    protected Intervention() {
    }

    Intervention(TicketSav ticket, LocalDateTime dateIntervention, String intervenant, String travaux) {
        this.ticket = ticket;
        this.dateIntervention = dateIntervention;
        this.intervenant = intervenant;
        this.travaux = travaux;
    }

    public Long getId() { return id; }
    public TicketSav getTicket() { return ticket; }
    public LocalDateTime getDateIntervention() { return dateIntervention; }
    public String getIntervenant() { return intervenant; }
    public String getTravaux() { return travaux; }
}

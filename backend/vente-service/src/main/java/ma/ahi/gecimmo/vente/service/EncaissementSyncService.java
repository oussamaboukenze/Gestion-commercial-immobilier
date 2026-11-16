package ma.ahi.gecimmo.vente.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ma.ahi.gecimmo.vente.domain.EncaissementRecu;
import ma.ahi.gecimmo.vente.domain.Reservation;
import ma.ahi.gecimmo.vente.messaging.Events.EncaissementEvent;
import ma.ahi.gecimmo.vente.repository.EncaissementRecuRepository;
import ma.ahi.gecimmo.vente.repository.ReservationRepository;

/** Met à jour le total encaissé d'un dossier à partir des événements de la trésorerie. */
@Service
public class EncaissementSyncService {

    private static final Logger log = LoggerFactory.getLogger(EncaissementSyncService.class);

    private final EncaissementRecuRepository encaissementRecuRepository;
    private final ReservationRepository reservationRepository;
    private final CompromisService compromisService;

    public EncaissementSyncService(EncaissementRecuRepository encaissementRecuRepository,
                                   ReservationRepository reservationRepository, CompromisService compromisService) {
        this.encaissementRecuRepository = encaissementRecuRepository;
        this.reservationRepository = reservationRepository;
        this.compromisService = compromisService;
    }

    @Transactional
    public void appliquer(EncaissementEvent event) {
        EncaissementRecu.Type type = "ANNULE".equalsIgnoreCase(event.type())
                ? EncaissementRecu.Type.ANNULE : EncaissementRecu.Type.ENREGISTRE;
        if (encaissementRecuRepository.existsByEncaissementIdAndType(event.encaissementId(), type)) {
            log.info("Encaissement {} ({}) déjà traité : ignoré", event.encaissementId(), type);
            return;
        }
        Reservation r = reservationRepository.findById(event.reservationId()).orElse(null);
        if (r == null) {
            log.warn("Encaissement {} : réservation {} inconnue", event.encaissementId(), event.reservationId());
            return;
        }
        // Contrainte unique (encaissement, type) : en cas de doublon simultané, la transaction échoue
        // et le message est rejoué : il sera alors ignoré par le test ci-dessus.
        encaissementRecuRepository.save(new EncaissementRecu(event.encaissementId(), type, r.getId(), event.montant()));
        r.ajouterEncaissement(type == EncaissementRecu.Type.ANNULE ? event.montant().negate() : event.montant());
        compromisService.repartir(r.getId());
        log.info("Dossier {} : total encaissé = {} DH", r.getNumero(), r.getTotalEncaisse());
    }
}

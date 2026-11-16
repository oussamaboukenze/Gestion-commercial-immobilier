package ma.ahi.gecimmo.referentiel.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import ma.ahi.gecimmo.referentiel.exception.BusinessException;
import ma.ahi.gecimmo.referentiel.service.LotService;

@Component
public class LotStatutListener {

    private static final Logger log = LoggerFactory.getLogger(LotStatutListener.class);

    private final LotService lotService;

    public LotStatutListener(LotService lotService) {
        this.lotService = lotService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_LOT_STATUT)
    public void onLotStatutChange(LotStatutChangeEvent event) {
        log.info("Evénement reçu : lot {} -> {} ({})", event.lotId(), event.nouveauStatut(), event.motif());
        try {
            lotService.changerStatut(event.lotId(), event.nouveauStatut(), event.statutsAttendus());
        } catch (BusinessException e) {
            // Transition refusée (ex. lot déjà re-réservé) : on journalise sans bloquer la file.
            log.warn("Changement de statut ignoré pour le lot {} : {}", event.lotId(), e.getMessage());
        }
    }
}

package ma.ahi.gecimmo.crm.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import ma.ahi.gecimmo.crm.service.ProspectService;

/** Réservation validée par l'ADV -> le prospect passe "CONVERTI" (taux de transformation). */
@Component
public class ReservationValideeListener {

    private static final Logger log = LoggerFactory.getLogger(ReservationValideeListener.class);

    private final ProspectService prospectService;

    public ReservationValideeListener(ProspectService prospectService) {
        this.prospectService = prospectService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_RESERVATION_VALIDEE)
    public void onReservationValidee(ReservationValideeEvent event) {
        log.info("Réservation {} validée : prospect {} converti", event.reservationId(), event.prospectId());
        prospectService.convertir(event.prospectId());
    }
}

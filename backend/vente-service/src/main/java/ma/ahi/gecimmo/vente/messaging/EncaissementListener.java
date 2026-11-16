package ma.ahi.gecimmo.vente.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import ma.ahi.gecimmo.vente.messaging.Events.EncaissementEvent;
import ma.ahi.gecimmo.vente.service.EncaissementSyncService;

/** Encaissement enregistré / chèque impayé / contre-passation -> total encaissé du dossier. */
@Component
public class EncaissementListener {

    private final EncaissementSyncService encaissementSyncService;

    public EncaissementListener(EncaissementSyncService encaissementSyncService) {
        this.encaissementSyncService = encaissementSyncService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_ENCAISSEMENTS)
    public void onEncaissement(EncaissementEvent event) {
        encaissementSyncService.appliquer(event);
    }
}

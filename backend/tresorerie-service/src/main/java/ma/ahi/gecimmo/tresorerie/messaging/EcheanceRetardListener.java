package ma.ahi.gecimmo.tresorerie.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import ma.ahi.gecimmo.tresorerie.messaging.Events.EcheanceEnRetardEvent;
import ma.ahi.gecimmo.tresorerie.service.RelanceService;

@Component
public class EcheanceRetardListener {

    private final RelanceService relanceService;

    public EcheanceRetardListener(RelanceService relanceService) {
        this.relanceService = relanceService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_ECHEANCES_RETARD)
    public void onEcheanceEnRetard(EcheanceEnRetardEvent event) {
        relanceService.traiterRetard(event);
    }
}

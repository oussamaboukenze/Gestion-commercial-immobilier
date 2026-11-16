package ma.ahi.gecimmo.tresorerie.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import ma.ahi.gecimmo.tresorerie.domain.Encaissement;
import ma.ahi.gecimmo.tresorerie.messaging.Events.EncaissementEvent;

/** Informe vente-service des encaissements (publié après le commit). */
@Component
public class TresorerieEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TresorerieEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public TresorerieEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void encaissementEnregistre(Encaissement e) {
        publierApresCommit(RabbitConfig.RK_ENCAISSEMENT_ENREGISTRE, new EncaissementEvent(e.getId(),
                e.getReservationId(), e.getEcheanceId(), e.getMontant(), "ENREGISTRE"));
    }

    public void encaissementAnnule(Encaissement e) {
        publierApresCommit(RabbitConfig.RK_ENCAISSEMENT_ANNULE, new EncaissementEvent(e.getId(),
                e.getReservationId(), e.getEcheanceId(), e.getMontant(), "ANNULE"));
    }

    private void publierApresCommit(String routingKey, Object event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envoyer(routingKey, event);
                }
            });
        } else {
            envoyer(routingKey, event);
        }
    }

    private void envoyer(String routingKey, Object event) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, routingKey, event);
        } catch (RuntimeException ex) {
            log.error("Echec de publication [{}] : {}", routingKey, event, ex);
        }
    }
}

package ma.ahi.gecimmo.vente.messaging;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import ma.ahi.gecimmo.vente.domain.LotStatut;
import ma.ahi.gecimmo.vente.messaging.Events.EcheanceEnRetardEvent;
import ma.ahi.gecimmo.vente.messaging.Events.LotStatutChangeEvent;
import ma.ahi.gecimmo.vente.messaging.Events.ReservationValideeEvent;

/**
 * Publie les événements APRES le commit de la transaction :
 * si l'enregistrement en base échoue, aucun message n'est envoyé.
 */
@Component
public class VenteEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(VenteEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public VenteEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void lotStatut(Long lotId, LotStatut nouveauStatut, List<LotStatut> attendus, String motif) {
        publierApresCommit(RabbitConfig.RK_LOT_STATUT, new LotStatutChangeEvent(lotId, nouveauStatut, attendus, motif));
    }

    /** Envoi immédiat (compensation quand la transaction locale échoue). */
    public void lotStatutImmediat(Long lotId, LotStatut nouveauStatut, List<LotStatut> attendus, String motif) {
        envoyer(RabbitConfig.RK_LOT_STATUT, new LotStatutChangeEvent(lotId, nouveauStatut, attendus, motif));
    }

    public void reservationValidee(ReservationValideeEvent event) {
        publierApresCommit(RabbitConfig.RK_RESERVATION_VALIDEE, event);
    }

    public void echeanceEnRetard(EcheanceEnRetardEvent event) {
        publierApresCommit(RabbitConfig.RK_ECHEANCE_RETARD, event);
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
            log.debug("Evénement publié [{}] : {}", routingKey, event);
        } catch (RuntimeException e) {
            // En production : pattern "outbox" pour ne jamais perdre un événement.
            log.error("Echec de publication [{}] : {}", routingKey, event, e);
        }
    }
}

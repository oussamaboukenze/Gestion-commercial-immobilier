package ma.ahi.gecimmo.postvente.messaging;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Evénements d'après-vente (journal d'activité, futurs abonnés). */
@Component
public class PostventeEventPublisher {

    public record ActeSigneEvent(Long reservationId, String reservationNumero, Long lotId, String lotCode,
                                 String numeroActe, LocalDate dateSignature) {
    }

    public record LotLivreEvent(Long reservationId, String reservationNumero, Long lotId, String lotCode,
                                LocalDate dateLivraison) {
    }

    private static final Logger log = LoggerFactory.getLogger(PostventeEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public PostventeEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void acteSigne(ActeSigneEvent event) {
        publier("acte.signe", event);
    }

    public void lotLivre(LotLivreEvent event) {
        publier("livraison.effectuee", event);
    }

    private void publier(String routingKey, Object event) {
        Runnable envoi = () -> {
            try {
                rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, routingKey, event);
            } catch (RuntimeException e) {
                log.error("Echec de publication [{}]", routingKey, e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envoi.run();
                }
            });
        } else {
            envoi.run();
        }
    }
}

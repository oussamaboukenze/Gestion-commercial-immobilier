package ma.ahi.gecimmo.admin.messaging;

import java.nio.charset.StandardCharsets;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import ma.ahi.gecimmo.admin.service.JournalService;

@Component
public class JournalListener {

    private final JournalService journalService;

    public JournalListener(JournalService journalService) {
        this.journalService = journalService;
    }

    /** Message brut : on garde le JSON tel quel, quel que soit l'émetteur. */
    @RabbitListener(queues = RabbitConfig.QUEUE_JOURNAL)
    public void onEvenement(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        journalService.tracer(routingKey == null ? "inconnu" : routingKey,
                new String(message.getBody(), StandardCharsets.UTF_8));
    }
}

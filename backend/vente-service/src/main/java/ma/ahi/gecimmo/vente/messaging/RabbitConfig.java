package ma.ahi.gecimmo.vente.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "gecimmo.events";

    public static final String RK_LOT_STATUT = "lot.statut";
    public static final String RK_RESERVATION_VALIDEE = "reservation.validee";
    public static final String RK_ECHEANCE_RETARD = "echeance.retard";

    /** Encaissements enregistrés / annulés par la trésorerie. */
    public static final String QUEUE_ENCAISSEMENTS = "vente.encaissements";
    public static final String RK_ENCAISSEMENTS = "encaissement.*";

    @Bean
    public TopicExchange gecimmoExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue encaissementsQueue() {
        return QueueBuilder.durable(QUEUE_ENCAISSEMENTS).build();
    }

    @Bean
    public Binding encaissementsBinding(Queue encaissementsQueue, TopicExchange gecimmoExchange) {
        return BindingBuilder.bind(encaissementsQueue).to(gecimmoExchange).with(RK_ENCAISSEMENTS);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}

package ma.ahi.gecimmo.referentiel.messaging;

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

/**
 * RabbitMQ : tous les services publient sur l'exchange "gecimmo.events".
 * Le référentiel écoute les changements de statut de lot envoyés par vente-service.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "gecimmo.events";
    public static final String QUEUE_LOT_STATUT = "referentiel.lot-statut";
    public static final String RK_LOT_STATUT = "lot.statut";

    @Bean
    public TopicExchange gecimmoExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue lotStatutQueue() {
        return QueueBuilder.durable(QUEUE_LOT_STATUT).build();
    }

    @Bean
    public Binding lotStatutBinding(Queue lotStatutQueue, TopicExchange gecimmoExchange) {
        return BindingBuilder.bind(lotStatutQueue).to(gecimmoExchange).with(RK_LOT_STATUT);
    }

    /** JSON + conversion vers le type du paramètre du @RabbitListener (pas de dépendance au package émetteur). */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}

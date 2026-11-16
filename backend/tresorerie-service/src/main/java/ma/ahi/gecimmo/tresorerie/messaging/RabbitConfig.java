package ma.ahi.gecimmo.tresorerie.messaging;

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
    public static final String RK_ENCAISSEMENT_ENREGISTRE = "encaissement.enregistre";
    public static final String RK_ENCAISSEMENT_ANNULE = "encaissement.annule";

    public static final String QUEUE_ECHEANCES_RETARD = "tresorerie.echeances-retard";
    public static final String RK_ECHEANCE_RETARD = "echeance.retard";

    @Bean
    public TopicExchange gecimmoExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue echeancesRetardQueue() {
        return QueueBuilder.durable(QUEUE_ECHEANCES_RETARD).build();
    }

    @Bean
    public Binding echeancesRetardBinding(Queue echeancesRetardQueue, TopicExchange gecimmoExchange) {
        return BindingBuilder.bind(echeancesRetardQueue).to(gecimmoExchange).with(RK_ECHEANCE_RETARD);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}

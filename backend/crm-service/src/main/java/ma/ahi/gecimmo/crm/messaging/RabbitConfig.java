package ma.ahi.gecimmo.crm.messaging;

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
    public static final String QUEUE_RESERVATION_VALIDEE = "crm.reservation-validee";
    public static final String RK_RESERVATION_VALIDEE = "reservation.validee";

    @Bean
    public TopicExchange gecimmoExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue reservationValideeQueue() {
        return QueueBuilder.durable(QUEUE_RESERVATION_VALIDEE).build();
    }

    @Bean
    public Binding reservationValideeBinding(Queue reservationValideeQueue, TopicExchange gecimmoExchange) {
        return BindingBuilder.bind(reservationValideeQueue).to(gecimmoExchange).with(RK_RESERVATION_VALIDEE);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}

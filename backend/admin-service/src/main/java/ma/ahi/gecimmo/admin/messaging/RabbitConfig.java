package ma.ahi.gecimmo.admin.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Le journal écoute TOUS les événements de l'exchange ("#"). */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "gecimmo.events";
    public static final String QUEUE_JOURNAL = "admin.journal";

    @Bean
    public TopicExchange gecimmoExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue journalQueue() {
        return QueueBuilder.durable(QUEUE_JOURNAL).build();
    }

    @Bean
    public Binding journalBinding(Queue journalQueue, TopicExchange gecimmoExchange) {
        return BindingBuilder.bind(journalQueue).to(gecimmoExchange).with("#");
    }
}

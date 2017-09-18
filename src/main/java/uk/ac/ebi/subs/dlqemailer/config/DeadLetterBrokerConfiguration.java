package uk.ac.ebi.subs.dlqemailer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration component to set up Dead Letter exchange/queues/bindings on RabbitMQ message broker.
 */
@Configuration
public class DeadLetterBrokerConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterBrokerConfiguration.class);

    private DLQEmailerProperties dlqEmailerProperties;

    public DeadLetterBrokerConfiguration(DLQEmailerProperties dlqEmailerProperties) {
        this.dlqEmailerProperties = dlqEmailerProperties;
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        String exchangeName = dlqEmailerProperties.getRabbitMQProp().getDeadLetterExchangeName();

        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue deadLetterEmailerQueue() {
        String queueName = dlqEmailerProperties.getRabbitMQProp().getDeadLetterEmailerQueueName();

        logger.info("[RabbitMQ]: Dead Letter Emailer queue has been created: {}", queueName);

        return QueueBuilder
                .durable(queueName)
                .build();
    }

    @Bean
    public Binding bindDeadLetterEmailerQueueToDeadLetterExchange(Queue deadLetterEmailerQueue, TopicExchange deadLetterExchange) {
        String deadLetterRoutingKey = dlqEmailerProperties.getRabbitMQProp().getDeadLetterRoutingKey();

        return BindingBuilder.bind(deadLetterEmailerQueue).to(deadLetterExchange)
                .with(deadLetterRoutingKey);
    }
}

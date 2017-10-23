package uk.ac.ebi.subs.dlqemailer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Reading property values from application.yml file.
 */
@Configuration
@ConfigurationProperties(prefix = "dlqEmailer")
@Data
public class DLQEmailerProperties {

    private RabbitMQProp rabbitMQProp;
    private Email email;

    @Data
    public static class RabbitMQProp {
        private String deadLetterExchangeName;
        private String deadLetterEmailerQueueName;
        private String deadLetterRoutingKey;
    }

    @Data
    public static class Email {
        private String from;
        private String to;
        private String replyTo;
        private long notificationScheduling;
    }
}
package uk.ac.ebi.subs.dlqemailer.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Reading property values from application.yml file.
 */
@Component
@EnableConfigurationProperties
@ConfigurationProperties("dlqEmailer")
@ToString
@Getter
@Setter
public class DLQEmailerProperties {

    private RabbitMQProp rabbitMQProp;

    @ToString
    @Getter
    @Setter
    public static class RabbitMQProp {
        private String baseURL;
        private String deadLetterExchangeName;
        private String deadLetterEmailerQueueName;
        private String deadLetterRoutingKey;
    }
}
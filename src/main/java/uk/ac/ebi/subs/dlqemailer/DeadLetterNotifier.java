package uk.ac.ebi.subs.dlqemailer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.dlqemailer.config.DLQEmailerProperties;
import uk.ac.ebi.subs.dlqemailer.config.DeadLetterData;
import uk.ac.ebi.subs.dlqemailer.config.EmailMustacheProperties;
import uk.ac.ebi.subs.dlqemailer.config.EnvironmentProperties;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DeadLetterNotifier {

    private Logger logger = LoggerFactory.getLogger(DeadLetterNotifier.class);

    @NonNull private EnvironmentProperties environmentProperties;
    @NonNull private DLQEmailerProperties dlqEmailerProperties;
    @NonNull private JavaMailSender emailSender;

    private final Map<String, DeadLetterData> deadLetters = new HashMap<>();

    @Value("${dlqEmailer.email.notificationScheduling}")
    private int notificationDuration;

    @RabbitListener(queues = "${dlqEmailer.rabbitMQProp.deadLetterEmailerQueueName}")
    public void consumeDeadLetterEmailerQueue(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();

        synchronized (deadLetters) {
            if (deadLetters.get(routingKey) == null) {
                deadLetters.put(routingKey, new DeadLetterData(new String(message.getBody()), 1));
            } else {
                DeadLetterData data = deadLetters.get(routingKey);
                data.incrementCount();
                deadLetters.replace(routingKey, data);
            }
        }
    }

    @Scheduled(fixedRateString = "${dlqEmailer.email.notificationScheduling}")
    public int sendNotification() throws IOException, MessagingException {
        logger.debug("send notification triggered.");
        final int messagesSize;

        Map<String, DeadLetterData> deadLettersToSend;
        ObjectMapper mapper = new ObjectMapper();

        synchronized (deadLetters) {
            deadLettersToSend = new HashMap<>(this.deadLetters);
            this.deadLetters.clear();
        }

        messagesSize = deadLettersToSend.size();
        if (messagesSize > 0) {
            logger.info("Sending an email");
            logger.debug("Dead Letters to send: {}", deadLettersToSend.toString());

            String emailBody = createEmailBody(deadLettersToSend);

            String messageAttachmentString = "";

            for (Map.Entry<String, DeadLetterData> message: deadLettersToSend.entrySet()) {
                String routingKey = message.getKey();
                Object json = mapper.readValue(message.getValue().getMessage(), Object.class);
                String indentedJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);

                messageAttachmentString += String.format("routing key: %s,\nmessage:\n%s\n", routingKey, indentedJson);
            }

            MimeMessage message = createMessage(emailBody, messageAttachmentString);

            emailSender.send(message);
        }

        return messagesSize;
    }

    private MimeMessage createMessage(String emailBody, String messageAttachmentString) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(message, true);

        DLQEmailerProperties.Email emailProp = dlqEmailerProperties.getEmail();
        messageHelper.setFrom(emailProp.getFrom());
        messageHelper.setTo(emailProp.getTo());
        messageHelper.setReplyTo(emailProp.getReplyTo());
        messageHelper.setText(emailBody);
        messageHelper.setSubject(MessageFormat.format("Message from Dead Letter Notifier {0}", environmentProperties.getName()));

        messageHelper.addAttachment("collectedMessages.txt",
                () -> new ByteArrayInputStream(messageAttachmentString.getBytes()));
        return message;
    }

    private String createEmailBody(Map<String, DeadLetterData> deadLettersToSend) throws IOException {
        MustacheFactory mustacheFactory = new DefaultMustacheFactory();
        Mustache mustache = mustacheFactory.compile("dead_letter_notifier_email_template.mustache");

        StringWriter writer = new StringWriter();
        mustache.execute(writer, getEmailMustacheProperties(deadLettersToSend)).flush();

        return writer.toString();
    }

    private EmailMustacheProperties getEmailMustacheProperties(Map<String, DeadLetterData> deadLettersToSend) throws UnknownHostException {
        EmailMustacheProperties emailMustacheProperties = new EmailMustacheProperties();
        emailMustacheProperties.setNumberOfMessages(
                String.valueOf(deadLettersToSend.values().stream().mapToLong( DeadLetterData::getCount).sum()));

        Set<Map.Entry<String,Long>> countByRoutingKey =
                deadLettersToSend.entrySet().stream()
                .map( entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().getCount()))
                .collect(Collectors.toSet());

        emailMustacheProperties.setCountByRoutingKey(countByRoutingKey);

        emailMustacheProperties.setConfigName(environmentProperties.getName());
        emailMustacheProperties.setHostname(java.net.InetAddress.getLocalHost().getHostName());
        emailMustacheProperties.setUsername(System.getProperty("user.name"));
        emailMustacheProperties.setMessageRate(dlqEmailerProperties.getEmail().getNotificationScheduling());

        Instant messageSendingEnd = Instant.now();
        Instant messageSendingStart = messageSendingEnd.minusMillis(notificationDuration);
        DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());

        emailMustacheProperties.setDateTime(
                DATE_TIME_FORMATTER.format(messageSendingStart) + " - " + DATE_TIME_FORMATTER.format(messageSendingEnd));
        return emailMustacheProperties;
    }
}

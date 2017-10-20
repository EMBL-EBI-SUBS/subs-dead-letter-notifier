package uk.ac.ebi.subs.dlqemailer;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.dlqemailer.config.DLQEmailerProperties;
import uk.ac.ebi.subs.dlqemailer.config.DeadLetterData;
import uk.ac.ebi.subs.dlqemailer.config.EmailMustacheProperties;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DeadLetterNotifier {

    private Logger logger = LoggerFactory.getLogger(DeadLetterNotifier.class);

    private DLQEmailerProperties dlqEmailerProperties;
    private JavaMailSender emailSender;

    public DeadLetterNotifier(DLQEmailerProperties dlqEmailerProperties, JavaMailSender emailSender) {
        this.dlqEmailerProperties = dlqEmailerProperties;
        this.emailSender = emailSender;
    }

    private Map<String, DeadLetterData> deadLetters = new HashMap<>();

    @RabbitListener(queues = "usi-dead-letter-emailer")
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

        Map<String, DeadLetterData> emptyDeadLetters = new HashMap<>();
        Map<String, DeadLetterData> deadLettersToSend;

        synchronized (deadLetters) {
            deadLettersToSend = this.deadLetters;
            this.deadLetters = emptyDeadLetters;
        }

        messagesSize = deadLettersToSend.size();
        if (messagesSize > 0) {
            logger.info("Sending an email");
            logger.debug("Dead Letters to send: {}", deadLettersToSend.toString());

            String emailBody = createEmailBody(deadLettersToSend);

            String messageAttachmentString = deadLettersToSend.entrySet()
                    .stream()
                    .map(message -> {
                        String routingKey = message.getKey();
                        DeadLetterData deadLetterData = message.getValue();
                        return String.format("routing key: %s, message: %s \n", routingKey, deadLetterData.getMessage());
                    })
                    .collect(Collectors.joining());

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
        messageHelper.setSubject("Message from Dead Letter Notifier");

        messageHelper.addAttachment("collectedMessages.txt",
                () -> new ByteArrayInputStream(messageAttachmentString.getBytes()));
        return message;
    }

    private String createEmailBody(Map<String, DeadLetterData> deadLettersToSend) throws IOException {
        MustacheFactory mustacheFactory = new DefaultMustacheFactory();
        Mustache mustache = mustacheFactory.compile("dead_letter_notifier_email_template.mustache");

        EmailMustacheProperties emailMustacheProperties = new EmailMustacheProperties();
        emailMustacheProperties.setNumberOfMessages(
                String.valueOf(deadLettersToSend.values().stream().mapToLong( DeadLetterData::getCount).sum()));

        Set<Map.Entry<String,Long>> countByRoutingKey =
                deadLettersToSend.entrySet().stream()
                .map( entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().getCount()))
                .collect(Collectors.toSet());

        emailMustacheProperties.setCountByRoutingKey(countByRoutingKey);

        StringWriter writer = new StringWriter();
        mustache.execute(writer, emailMustacheProperties).flush();

        return writer.toString();
    }
}

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
import uk.ac.ebi.subs.dlqemailer.config.EmailMustacheProperties;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
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

    private Map<String, String> messages = new HashMap<>();
    private Map<String, Long> messageCountByRoutingKey = new HashMap<>();

    @RabbitListener(queues = "usi-dead-letter-emailer")
    public void consumeDeadLetterEmailerQueue(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        messages.putIfAbsent(routingKey, new String(message.getBody()));
        messageCountByRoutingKey.merge(routingKey, (long) 1, (a, b) -> a + b);
    }

    @Scheduled(fixedRateString = "${dlqEmailer.email.notificationScheduling}")
    public int sendNotification() throws IOException, MessagingException {
        logger.info("send notification triggered.");
        final int messagesSize;

        synchronized (messages) {
            messagesSize = messages.size();
            if (messagesSize > 0) {
                logger.info("Sending an email");

                String emailBody = createEmailBody();

                String messageAttachmentString = messages.entrySet()
                        .stream()
                        .map(message -> "routing key: " + message.getKey().toString() + ", message: " + message.getValue().toString() + "\n")
                        .collect(Collectors.joining());

                MimeMessage message = createMessage(emailBody, messageAttachmentString);

                emailSender.send(message);

                messages.clear();
                messageCountByRoutingKey.clear();
            }
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

    private String createEmailBody() throws IOException {
        MustacheFactory mustacheFactory = new DefaultMustacheFactory();
        Mustache mustache = mustacheFactory.compile("dead_letter_notifier_email_template.mustache");

        EmailMustacheProperties emailMustacheProperties = new EmailMustacheProperties();
        emailMustacheProperties.setNumberOfMessages(
                String.valueOf(messageCountByRoutingKey.values().stream().mapToInt(Number::intValue).sum()));
        emailMustacheProperties.setCountByRoutingKey(messageCountByRoutingKey.entrySet());

        StringWriter writer = new StringWriter();
        mustache.execute(writer, emailMustacheProperties).flush();

        return writer.toString();
    }
}

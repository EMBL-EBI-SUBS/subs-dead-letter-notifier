package uk.ac.ebi.subs.dlqemailer;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.dlqemailer.config.DLQEmailerProperties;
import uk.ac.ebi.subs.dlqemailer.config.EmailMustacheProperties;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DeadLetterNotifier {

    private Logger logger = LoggerFactory.getLogger(DeadLetterNotifier.class);

    private DLQEmailerProperties dlqEmailerProperties;
    private JavaMailSender emailSender;

    public DeadLetterNotifier(DLQEmailerProperties dlqEmailerProperties, JavaMailSender emailSender) {
        this.dlqEmailerProperties = dlqEmailerProperties;
        this.emailSender = emailSender;
    }

    private LocalTime initialTime;
    private List<String> messages = new ArrayList<>();

    public void sendNotification() throws IOException, MessagingException {
        if (messages.size() <= 0 ) {
            return;
        }

        String emailBody = createEmailBody();

        String messageAttachmentString = String.join("\n", messages);

        MimeMessage message = createMessage(emailBody, messageAttachmentString);

        emailSender.send(message);
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
        long messageCount = messages.size();

        MustacheFactory mustacheFactory = new DefaultMustacheFactory();
        Mustache mustache = mustacheFactory.compile("dead_letter_notifier_email_template.mustache");

        EmailMustacheProperties emailMustacheProperties = new EmailMustacheProperties();
        emailMustacheProperties.setNumberOfMessages(String.valueOf(messageCount));

        StringWriter writer = new StringWriter();
        mustache.execute(writer, emailMustacheProperties).flush();

        return writer.toString();
    }

    private void setInitialTime() {
        if (initialTime == null) {
            initialTime = LocalTime.now();
        }
    }
}

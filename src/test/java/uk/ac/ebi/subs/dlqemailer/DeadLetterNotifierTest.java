package uk.ac.ebi.subs.dlqemailer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.junit4.SpringRunner;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DeadLetterNotifierTest {

    @Autowired
    private DeadLetterNotifier notifier;

    @SpyBean
    private JavaMailSender emailSender;

    @Test
    public void whenNoMessageRecieved_ThenMessageCountShouldBeZero()
            throws IOException, MessagingException {
        assertThat(notifier.sendNotification(), is(equalTo(0)));
    }

    @Test
    public void whenReceiving10MessagesWith3DifferentRoutingKeys_ThenMessageCountShouldBe3()
            throws IOException, MessagingException {
        doNothing().when(emailSender).send(any(MimeMessage.class));

        generateMessage(3, generateMessageProperty(1)).forEach(
                message -> notifier.consumeDeadLetterEmailerQueue(message)
        );
        generateMessage(3, generateMessageProperty(2)).forEach(
                message -> notifier.consumeDeadLetterEmailerQueue(message)
        );
        generateMessage(4, generateMessageProperty(3)).forEach(
                message -> notifier.consumeDeadLetterEmailerQueue(message)
        );

        assertThat(notifier.sendNotification(), is(equalTo(3)));
    }

    private MessageProperties generateMessageProperty(int routingKeyId) {
        MessageProperties mp = new MessageProperties();
        mp.setReceivedRoutingKey("routingKey" + routingKeyId);
        return mp;
    }

    private List<Message> generateMessage(int numberOfMessage, MessageProperties messageProperties) {
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < numberOfMessage; i++) {
            Message message = new Message(("Message" + i).getBytes(), messageProperties);
            messages.add(message);
        }

        return messages;
    }
}

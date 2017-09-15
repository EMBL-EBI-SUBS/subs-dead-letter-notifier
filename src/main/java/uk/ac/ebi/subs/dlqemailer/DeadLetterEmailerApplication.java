package uk.ac.ebi.subs.dlqemailer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("uk.ac.ebi.subs.dlqemailer")
public class DeadLetterEmailerApplication implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterEmailerApplication.class);

    @Autowired
    private DeadLetterNotifier notifier;

    public static void main(String[] args) {
        SpringApplication.run(DeadLetterEmailerApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Application has started");

        notifier.sendNotification();
    }
}
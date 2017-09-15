package uk.ac.ebi.subs.dlqemailer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan("uk.ac.ebi.subs.dlqemailer")
@EnableScheduling
public class DeadLetterEmailerApplication {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterEmailerApplication.class);

    @Autowired
    private DeadLetterNotifier notifier;

    public static void main(String[] args) {
        logger.info("Dead Letter Emailer application has started");
        SpringApplication.run(DeadLetterEmailerApplication.class, args);
    }
}
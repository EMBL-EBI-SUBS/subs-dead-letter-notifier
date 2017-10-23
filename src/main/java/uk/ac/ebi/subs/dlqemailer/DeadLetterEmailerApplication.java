package uk.ac.ebi.subs.dlqemailer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationPidFileWriter;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DeadLetterEmailerApplication {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterEmailerApplication.class);

    public static void main(String[] args) {
        logger.info("Dead Letter Emailer application has started");

        SpringApplication springApplication = new SpringApplication( DeadLetterEmailerApplication.class);
        ApplicationPidFileWriter applicationPidFileWriter = new ApplicationPidFileWriter();
        springApplication.addListeners( applicationPidFileWriter );
        springApplication.run(args);
    }
}
package uk.ac.ebi.subs.dlqemailer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("uk.ac.ebi.subs.dlqemailer")
public class DeadLetterEmailerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeadLetterEmailerApplication.class, args);
    }
}
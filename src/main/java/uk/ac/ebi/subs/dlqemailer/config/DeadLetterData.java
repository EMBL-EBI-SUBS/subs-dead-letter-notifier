package uk.ac.ebi.subs.dlqemailer.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DeadLetterData {
    private String message;
    private long count;

    public void incrementCount() {
        count++;
    }
}

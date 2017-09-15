package uk.ac.ebi.subs.dlqemailer.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class MessageHolder {
    private String message;
    private String routingKey;

    @Override
    public String toString() {
        return "routingKey = " + routingKey + ", message=" + message + "\n";
    }
}

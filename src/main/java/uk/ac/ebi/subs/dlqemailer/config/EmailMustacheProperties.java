package uk.ac.ebi.subs.dlqemailer.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;
import java.util.Set;

@Getter
@Setter
@ToString
public class EmailMustacheProperties {

    private String numberOfMessages;
    private Set<Map.Entry<String,Long>> countByRoutingKey;
}

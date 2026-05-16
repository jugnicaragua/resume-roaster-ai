package ni.jug.resumeroaster.configuration.properties;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author jxareas
 */
@Data
@Validated
@ConfigurationProperties(prefix = "corenlp")
public class CoreNlpConfigurationProperties {

    @NotBlank
    private String annotators = "tokenize,ssplit,pos,lemma,ner";

    private boolean useSuTime = false;

    @NotEmpty
    private Set<String> targetTags = new LinkedHashSet<>(Set.of(
            "PERSON", "EMAIL", "URL", "PHONE_NUMBER", "CITY", "STATE_OR_PROVINCE"
    ));

    @NotBlank
    private String redactionPlaceholder = "[REDACTED]";

    private Map<String, String> extraProperties = new HashMap<>();
}

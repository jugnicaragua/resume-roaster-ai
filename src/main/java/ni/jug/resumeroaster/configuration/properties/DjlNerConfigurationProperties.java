package ni.jug.resumeroaster.configuration.properties;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author jxareas
 */
@Data
@Validated
@ConfigurationProperties(prefix = "djl.ner")
public class DjlNerConfigurationProperties {

    @NotEmpty
    private Set<String> targetTags = new LinkedHashSet<>(Set.of("PER", "LOC"));

    private double confidenceCutoff = 0.5;
}

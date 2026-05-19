package ni.jug.resumeroaster.service;

import lombok.extern.slf4j.Slf4j;
import ni.jug.resumeroaster.configuration.properties.CoreNlpConfigurationProperties;
import ni.jug.resumeroaster.model.EntityMention;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author jxareas
 */
@Slf4j
@Service
public class EntityTextRedactor implements TextRedactor {

    private final CoreNlpConfigurationProperties properties;

    public EntityTextRedactor(CoreNlpConfigurationProperties properties) {
        this.properties = properties;
    }

    @Override
    public String redact(String text, List<EntityMention> entities) {
        log.debug("Running PII redaction on {} characters with {} pre-computed entities", text.length(), entities.size());
        String placeholder = properties.getRedactionPlaceholder();
        String result = text;
        for (EntityMention entity : entities) {
            result = result.replace(entity.text(), placeholder);
        }
        log.debug("PII redaction complete");
        return result;
    }
}

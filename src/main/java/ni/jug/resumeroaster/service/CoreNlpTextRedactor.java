package ni.jug.resumeroaster.service;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreEntityMention;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import lombok.extern.slf4j.Slf4j;
import ni.jug.resumeroaster.configuration.properties.CoreNlpConfigurationProperties;
import ni.jug.resumeroaster.model.NameEntity;
import ni.jug.resumeroaster.model.Redaction;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author jxareas
 */
@Slf4j
@Service
public class CoreNlpTextRedactor implements TextRedactor {

    private final StanfordCoreNLP pipeline;
    private final CoreNlpConfigurationProperties properties;

    public CoreNlpTextRedactor(@Lazy StanfordCoreNLP pipeline, CoreNlpConfigurationProperties properties) {
        this.pipeline = pipeline;
        this.properties = properties;
    }

    @Override
    public Redaction redactText(String text) {
        log.debug("Running PII redaction on {} characters", text.length());
        CoreDocument document = new CoreDocument(text);
        pipeline.annotate(document);
        Redaction result = redact(text, document.entityMentions());
        log.debug("Redacted {} entities", result.recognizedEntities().size());
        return result;
    }

    Redaction redact(String text, List<CoreEntityMention> mentions) {
        Set<String> targetTags = properties.getTargetTags();
        String replacementTemplate = properties.getReplacementTemplate();

        List<NameEntity> detectedEntities = mentions.stream()
                .filter(m -> targetTags.contains(m.entityType()))
                .map(m -> new NameEntity(m.text(), m.entityType()))
                .filter(e -> !e.name().isEmpty())
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                e -> e.entity() + ":" + e.name().toLowerCase(),
                                e -> e,
                                (existing, duplicate) -> existing,
                                LinkedHashMap::new
                        ),
                        m -> List.copyOf(m.values())
                ));

        StringBuilder builder = new StringBuilder(text);
        mentions.stream()
                .filter(m -> targetTags.contains(m.entityType()))
                .sorted(Comparator.comparingInt((CoreEntityMention m) -> m.charOffsets().first).reversed())
                .forEach(mention -> {
                    int start = mention.charOffsets().first;
                    int end = mention.charOffsets().second;
                    builder.replace(start, end, replacementTemplate.formatted(mention.entityType()));
                });

        return new Redaction(builder.toString(), detectedEntities);
    }
}

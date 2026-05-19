package ni.jug.resumeroaster.service.ner;

import lombok.RequiredArgsConstructor;
import ni.jug.resumeroaster.ai.annotations.ClassicalNlpNer;
import ni.jug.resumeroaster.ai.model.NerModel;
import ni.jug.resumeroaster.configuration.properties.CoreNlpConfigurationProperties;
import ni.jug.resumeroaster.model.EntityMention;
import ni.jug.resumeroaster.model.NerInferenceBackend;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author jxareas
 */
@Component
@RequiredArgsConstructor
public class CoreNlpNerStrategy implements NerStrategy {

    @ClassicalNlpNer
    private final NerModel nerModel;
    private final CoreNlpConfigurationProperties corenlpConfig;

    @Override
    public NerInferenceBackend backend() {
        return NerInferenceBackend.CORENLP;
    }

    @Override
    public List<EntityMention> detect(String text) {
        return nerModel.inferChunked(text).entities().stream()
                .filter(e -> corenlpConfig.getTargetTags().contains(e.type()))
                .toList();
    }
}

package ni.jug.resumeroaster.service.ner;

import lombok.RequiredArgsConstructor;
import ni.jug.resumeroaster.ai.annotations.NeuralNer;
import ni.jug.resumeroaster.ai.model.NerModel;
import ni.jug.resumeroaster.configuration.properties.DjlNerConfigurationProperties;
import ni.jug.resumeroaster.model.EntityMention;
import ni.jug.resumeroaster.model.EntityRecognitionMethod;
import ni.jug.resumeroaster.model.NerInferenceBackend;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author jxareas
 */
@Component
@RequiredArgsConstructor
public class DjlNerStrategy implements NerStrategy {

    @NeuralNer
    private final NerModel nerModel;
    private final DjlNerConfigurationProperties djlNerConfig;

    @Override
    public NerInferenceBackend backend() {
        return NerInferenceBackend.DJL;
    }

    @Override
    public List<EntityMention> detect(String text) {
        return nerModel.inferChunked(text).entities().stream()
                .filter(e -> djlNerConfig.getTargetTags().contains(e.type()))
                .filter(e -> e.entityRecognitionMethod() == EntityRecognitionMethod.PATTERN_MATCHING
                        || e.confidence() >= djlNerConfig.getConfidenceCutoff())
                .toList();
    }
}

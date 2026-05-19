package ni.jug.resumeroaster.service.ner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ni.jug.resumeroaster.ai.annotations.NeuralNer;
import ni.jug.resumeroaster.ai.model.NerModel;
import ni.jug.resumeroaster.configuration.properties.DjlNerConfigurationProperties;
import ni.jug.resumeroaster.model.EntityMention;
import ni.jug.resumeroaster.model.NerInferenceBackend;
import ni.jug.resumeroaster.service.PatternMatchingDetector;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jxareas
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HybridNerStrategy implements NerStrategy {

    @NeuralNer
    private final NerModel nerModel;
    private final DjlNerConfigurationProperties djlNerConfig;
    private final List<PatternMatchingDetector> patternDetectors;

    @Override
    public NerInferenceBackend backend() {
        return NerInferenceBackend.DJL_REGEX;
    }

    @Override
    public List<EntityMention> detect(String text) {
        List<EntityMention> neural = nerModel.inferChunked(text).entities().stream()
                .filter(e -> djlNerConfig.getTargetTags().contains(e.type()))
                .filter(e -> e.confidence() >= djlNerConfig.getConfidenceCutoff())
                .toList();

        List<EntityMention> patternDetected = patternDetectors.stream()
                .flatMap(d -> d.detect(text).stream())
                .toList();

        List<EntityMention> merged = new ArrayList<>(neural.size() + patternDetected.size());
        merged.addAll(neural);
        merged.addAll(patternDetected);
        log.debug("Hybrid NER: {} neural + {} from pattern detectors", neural.size(), patternDetected.size());
        return EntityMention.deduplicate(merged);
    }
}

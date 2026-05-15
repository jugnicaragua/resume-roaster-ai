package ni.jug.resumeroaster.ai.model;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import ni.jug.resumeroaster.model.DetectionMethod;
import ni.jug.resumeroaster.model.EntityMention;
import ni.jug.resumeroaster.model.NerResponse;
import ni.jug.resumeroaster.ai.annotations.ClassicalNlpNer;
import org.springframework.context.annotation.Lazy;

import java.util.List;

/**
 * NER inference service backed by Stanford CoreNLP's statistical and pattern-matching detection methods.
 *
 * <p>Runs the standard {@code tokenize → ssplit → pos → lemma → ner} annotator chain,
 * which combines a Conditional Random Field (CRF) sequence model with pattern-matching
 * token-level patterns. The detection method on each {@link EntityMention} is inferred from
 * the confidence map: spans with a non-zero CRF probability are tagged
 * {@link DetectionMethod#STATISTICAL_MODEL}; spans recognized purely by patterns carry
 * zero probability and are tagged {@link DetectionMethod#PATTERN_MATCHING}.
 *
 * @author jxareas
 * @see DetectionMethod#STATISTICAL_MODEL
 * @see DetectionMethod#PATTERN_MATCHING
 */
@ClassicalNlpNer
public class CoreNlpNerModel implements NerModel {

    /**
     * Pre-configured Stanford CoreNLP pipeline. Initialized lazily because loading the
     * CRF models is expensive at startup; Spring resolves the proxy on first use.
     */
    private final StanfordCoreNLP pipeline;

    /**
     * @param pipeline lazily initialized Stanford CoreNLP pipeline
     */
    public CoreNlpNerModel(@Lazy StanfordCoreNLP pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * Annotates {@code text} with the CoreNLP pipeline and maps each recognized entity
     * mention to an {@link EntityMention}, inferring the detection method from whether
     * the CRF assigned a non-zero confidence to the predicted entity type.
     *
     * @param text raw input string to run NER over
     * @return a {@link NerResponse} containing the original text and all extracted entity
     *         mentions in document order
     */
    public NerResponse infer(String text) {
        CoreDocument document = new CoreDocument(text);
        pipeline.annotate(document);

        List<EntityMention> entities = document.entityMentions().stream()
                .map(mention -> {
                    double confidence = mention.entityTypeConfidences().getOrDefault(mention.entityType(), 0.0);
                    DetectionMethod detectionMethod = confidence > 0.0 ? DetectionMethod.STATISTICAL_MODEL : DetectionMethod.PATTERN_MATCHING;
                    return new EntityMention(
                            mention.text(),
                            mention.entityType(),
                            confidence,
                            detectionMethod,
                            mention.charOffsets().first,
                            mention.charOffsets().second
                    );
                })
                .toList();

        return new NerResponse(text, entities);
    }
}

package ni.jug.resumeroaster.ai.model;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import ni.jug.resumeroaster.model.EntityMention;
import ni.jug.resumeroaster.model.NerResponse;
import ni.jug.resumeroaster.model.NerSource;
import ni.jug.resumeroaster.ai.annotations.ClassicalNlpNer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;

import java.util.List;

/**
 * NER inference service backed by Stanford CoreNLP's statistical / rule-based annotation pipeline.
 *
 * <p>Runs the standard {@code tokenize → ssplit → pos → lemma → ner} annotator chain,
 * which combines a Conditional Random Field (CRF) sequence model with rule-based
 * token-level patterns. The source tag on each {@link EntityMention} is inferred from
 * the confidence map: spans with a non-zero CRF probability are tagged
 * {@link NerSource#CONDITIONAL_RANDOM_FIELD}; spans recognized purely by rules carry
 * zero probability and are tagged {@link NerSource#RULE_BASED}.
 *
 * @author jxareas
 * @see NerSource#CONDITIONAL_RANDOM_FIELD
 * @see NerSource#RULE_BASED
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
     * mention to an {@link EntityMention}, inferring the {@link NerSource} from whether
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
                    NerSource source = confidence > 0.0 ? NerSource.CONDITIONAL_RANDOM_FIELD : NerSource.RULE_BASED;
                    return new EntityMention(
                            mention.text(),
                            mention.entityType(),
                            confidence,
                            source,
                            mention.charOffsets().first,
                            mention.charOffsets().second
                    );
                })
                .toList();

        return new NerResponse(text, entities);
    }
}

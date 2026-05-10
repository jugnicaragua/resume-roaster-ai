package ni.jug.resumeroaster.service;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import ni.jug.resumeroaster.model.EntityMention;
import ni.jug.resumeroaster.model.NerResponse;
import ni.jug.resumeroaster.model.NerSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author jxareas
 */
@Service
public class NerInferenceService {

    private final StanfordCoreNLP pipeline;

    public NerInferenceService(@Lazy StanfordCoreNLP pipeline) {
        this.pipeline = pipeline;
    }

    public NerResponse infer(String text) {
        CoreDocument document = new CoreDocument(text);
        pipeline.annotate(document);

        List<EntityMention> entities = document.entityMentions().stream()
                .map(mention -> {
                    double confidence = mention.entityTypeConfidences().getOrDefault(mention.entityType(), 0.0);
                    NerSource source = confidence > 0.0 ? NerSource.PROBABILISTIC : NerSource.RULE_BASED;
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

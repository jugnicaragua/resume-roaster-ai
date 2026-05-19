package ni.jug.resumeroaster.service.ner;

import lombok.RequiredArgsConstructor;
import ni.jug.resumeroaster.ai.annotations.NeuralNer;
import ni.jug.resumeroaster.ai.model.NerModel;
import ni.jug.resumeroaster.model.EntityMention;
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

    @Override
    public NerInferenceBackend backend() {
        return NerInferenceBackend.DJL;
    }

    @Override
    public List<EntityMention> detect(String text) {
        return nerModel.inferChunked(text).entities();
    }
}

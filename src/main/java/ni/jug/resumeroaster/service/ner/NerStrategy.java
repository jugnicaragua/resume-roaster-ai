package ni.jug.resumeroaster.service.ner;

import ni.jug.resumeroaster.model.EntityMention;
import ni.jug.resumeroaster.model.NerInferenceBackend;

import java.util.List;

/**
 * @author jxareas
 */
public interface NerStrategy {

    NerInferenceBackend backend();

    List<EntityMention> detect(String text);
}

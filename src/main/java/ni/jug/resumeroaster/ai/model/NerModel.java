package ni.jug.resumeroaster.ai.model;

import ni.jug.resumeroaster.model.NerResponse;

/**
 * Named entity recognition model: accepts raw text and returns detected entity spans
 * with type, confidence score, and character offsets into the original string.
 *
 * @author jxareas
 * @see CoreNlpNerModel
 * @see TransformerTokenClassificationNerModel
 */
public interface NerModel extends InferenceModel<String, NerResponse> {
}

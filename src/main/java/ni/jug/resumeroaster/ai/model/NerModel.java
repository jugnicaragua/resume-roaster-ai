package ni.jug.resumeroaster.ai.model;

import ni.jug.resumeroaster.model.NerResponse;

/**
 * Named entity recognition model: accepts raw text and returns detected entity spans
 * with type, confidence score, and character offsets into the original string.
 *
 * @author jxareas
 * @see CoreNlpNerModel
 * @see TransformerNerModel
 */
public interface NerModel extends InferenceModel<String, NerResponse> {

    /**
     * Runs NER over arbitrarily long text, chunking the input if necessary.
     * The default implementation delegates directly to {@link #infer} — models without
     * a fixed token-length limit (e.g., CoreNLP) can rely on this no-op default.
     * Models with a hard sequence-length cap (e.g., ONNX transformer) should override
     * this method to split the input into overlapping windows before inference.
     *
     * @param text raw input string, any length
     * @return merged {@link NerResponse} over the full text
     */
    default NerResponse inferChunked(String text) {
        return infer(text);
    }
}

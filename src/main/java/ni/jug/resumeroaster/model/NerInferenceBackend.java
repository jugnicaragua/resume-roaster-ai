package ni.jug.resumeroaster.model;

/**
 * Selects which NER backend is used during the resume roasting pipeline.
 *
 * @author jxareas
 */
public enum NerInferenceBackend {

    /** Stanford CoreNLP — CRF + pattern-matching, no token-length limit. */
    CORENLP,

    /** DJL transformer (DistilBERT-NER via ONNX) — deep-learning, chunked inference. */
    DJL,

    /**
     * Hybrid: DJL transformer for person detection (PER) combined with CoreNLP
     * pattern-matching for structured PII (emails, phones, URLs).
     */
    DJL_REGEX
}

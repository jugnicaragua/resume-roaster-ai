package ni.jug.resumeroaster.model;

/**
 * Identifies the detection method (inference mechanism) used to identify an {@link EntityMention}.
 *
 * <p>Three distinct NER backends are supported, each with a different modeling approach:
 * <ul>
 *   <li>{@link #STATISTICAL_MODEL} — Stanford CoreNLP's {@code CRFClassifier}, a linear-chain
 *       CRF that scores label sequences using hand-crafted features and Viterbi decoding.</li>
 *   <li>{@link #PATTERN_MATCHING} — deterministic regex patterns applied directly to raw text,
 *       with no statistical component.</li>
 *   <li>{@link #TRANSFORMER} — a DistilBERT transformer fine-tuned on CoNLL-2003 and exported
 *       to ONNX, run via DJL and ONNX Runtime.</li>
 * </ul>
 *
 * @author jxareas
 */
public enum DetectionMethod {

    /**
     * Statistical model: Stanford CoreNLP CRF-based NER ({@code CRFClassifier} with Viterbi decoding).
     */
    STATISTICAL_MODEL,

    /**
     * Pattern matching: Deterministic regex-based NER applied directly to raw text.
     */
    PATTERN_MATCHING,

    /**
     * Transformer-based detection: DistilBERT or other neural models.
     */
    TRANSFORMER,
}

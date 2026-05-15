package ni.jug.resumeroaster.model;

/**
 * Identifies the detection method (inference mechanism) used to identify an {@link EntityMention}.
 *
 * <p>Three distinct NER backends are supported, each with a different modeling approach:
 * <ul>
 *   <li>{@link #CONDITIONAL_RANDOM_FIELD} — Stanford CoreNLP's {@code CRFClassifier}, a linear-chain
 *       CRF that scores label sequences using hand-crafted features and Viterbi decoding.</li>
 *   <li>{@link #RULE_BASED_PATTERN_MATCHING} — deterministic regex patterns applied directly to raw text,
 *       with no statistical component.</li>
 *   <li>{@link #TRANSFORMER_DEEP_LEARNING} — a DistilBERT transformer fine-tuned on CoNLL-2003 and exported
 *       to ONNX, run via DJL and ONNX Runtime.</li>
 * </ul>
 *
 * @author jxareas
 */
public enum DetectionMethod {

    /**
     * Statistical model: Stanford CoreNLP CRF-based NER ({@code CRFClassifier} with Viterbi decoding).
     */
    CONDITIONAL_RANDOM_FIELD,

    /**
     * Pattern matching: Deterministic regex-based NER applied directly to raw text.
     */
    RULE_BASED_PATTERN_MATCHING,

    /**
     * Transformer-based detection: DistilBERT or other neural models.
     */
    TRANSFORMER_DEEP_LEARNING,
}

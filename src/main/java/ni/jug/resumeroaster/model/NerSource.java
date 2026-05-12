package ni.jug.resumeroaster.model;

/**
 * Identifies the underlying inference mechanism used to produce a {@link EntityMention}.
 *
 * <p>Three distinct NER backends are supported, each with a different modeling approach:
 * <ul>
 *   <li>{@link #CONDITIONAL_RANDOM_FIELD} — Stanford CoreNLP's {@code CRFClassifier}, a linear-chain
 *       CRF that scores label sequences using hand-crafted features and Viterbi decoding.</li>
 *   <li>{@link #RULE_BASED} — deterministic regex patterns applied directly to raw text,
 *       with no statistical component.</li>
 *   <li>{@link #DEEP_LEARNING} — a DistilBERT transformer fine-tuned on CoNLL-2003 and exported
 *       to ONNX, run via DJL and ONNX Runtime.</li>
 * </ul>
 *
 * @author jxareas
 */
public enum NerSource {

    /**
     * Stanford CoreNLP CRF-based NER ({@code CRFClassifier} with Viterbi decoding).
     */
    CONDITIONAL_RANDOM_FIELD,

    /**
     * Deterministic rule-based NER using regular expression pattern matching.
     */
    RULE_BASED,

    /**
     * Deep-learning models: transformers, artificial neural networks, etc.
     */
    DEEP_LEARNING,
}

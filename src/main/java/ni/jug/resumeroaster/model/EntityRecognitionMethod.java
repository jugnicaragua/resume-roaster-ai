package ni.jug.resumeroaster.model;

/**
 * Identifies the entity recognition method (inference mechanism) used to identify an {@link EntityMention}.
 *
 * <p>Three distinct NER backends are supported, each with a different modeling approach:
 * <ul>
 *   <li>{@link #CONDITIONAL_RANDOM_FIELD} — Stanford CoreNLP's {@code CRFClassifier}, a linear-chain
 *       CRF that scores label sequences using hand-crafted features and Viterbi decoding.</li>
 *   <li>{@link #PATTERN_MATCHING} — deterministic regex patterns applied directly to raw text,
 *       with no statistical component.</li>
 *   <li>{@link #DEEP_LEARNING_TRANSFORMER} — Transformer-based token classification.</li>
 * </ul>
 *
 * @author jxareas
 */
public enum EntityRecognitionMethod {

    /**
     * Uses a conditional random field: a class of probabilistic graphical models designed specifically for sequence
     * modeling
     *
     * <p>This corresponds to Stanford CoreNLP's {@code CRFClassifier}, which labels
     * tokens using learned statistical features and Viterbi decoding.
     */
    CONDITIONAL_RANDOM_FIELD,

    /**
     * Uses deterministic pattern-based rules.
     *
     * <p>This method applies regular expressions, dictionaries, or other explicit
     * text patterns directly to the input text rather than relying on statistical learning.
     */
    PATTERN_MATCHING,

    /**
     * Uses a transformer-based neural model for named-entity recognition.
     *
     * <p>This includes models such as DistilBERT or similar transformer architectures
     * that infer entities from contextual token representations.
     */
    DEEP_LEARNING_TRANSFORMER,
}

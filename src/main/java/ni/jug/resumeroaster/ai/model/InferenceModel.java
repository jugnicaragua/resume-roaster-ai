package ni.jug.resumeroaster.ai.model;

/**
 * Generic functional interface for AI inference models.
 * Takes an input of type {@code R} and produces a result of type {@code T}.
 *
 * @param <T> input type
 * @param <R> result type
 * @author jxareas
 */
@FunctionalInterface
public interface InferenceModel<T, R> {

    R infer(T t);
}

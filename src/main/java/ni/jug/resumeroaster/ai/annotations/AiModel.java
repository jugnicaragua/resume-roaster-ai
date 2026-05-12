package ni.jug.resumeroaster.ai.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker for all AI-model inference classes in this project.
 * Applied as a meta-annotation on specific model qualifier annotations
 * (e.g., {@link ClassicalNlpNer}, {@link NeuralNer}).
 *
 * @author jxareas
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AiModel {
}

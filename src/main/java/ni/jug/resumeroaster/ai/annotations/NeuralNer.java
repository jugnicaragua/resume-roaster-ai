package ni.jug.resumeroaster.ai.annotations;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Qualifier for the neural NER model — a deep-learning sequence-labeling pipeline
 * backed by a transformer encoder exported to ONNX and run via DJL.
 *
 * @author jxareas
 * @see AiModel
 */
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@Qualifier
@AiModel
public @interface NeuralNer {
}

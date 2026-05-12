package ni.jug.resumeroaster.ai.annotations;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Qualifier for the classical NLP-based NER model — a hybrid pipeline combining
 * Conditional Random Field (CRF) sequence labeling with rule-based token patterns
 * (e.g., Stanford CoreNLP).
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
public @interface ClassicalNlpNer {
}

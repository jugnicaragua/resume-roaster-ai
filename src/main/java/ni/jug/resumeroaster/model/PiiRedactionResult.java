package ni.jug.resumeroaster.model;

import java.util.List;

/**
 * @author jxareas
 */
public record PiiRedactionResult(String redactedText, List<EntityMention> entities) {
}

package ni.jug.resumeroaster.model;

import java.util.List;

/**
 * @author jxareas
 */
public record Redaction(String redactedText, List<NameEntity> recognizedEntities) {
}

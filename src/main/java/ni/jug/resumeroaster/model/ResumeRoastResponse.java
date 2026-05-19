package ni.jug.resumeroaster.model;

import java.util.List;

/**
 * @author jxareas
 */
public record ResumeRoastResponse(String extractedText, String redactedText, String answer, List<EntityMention> entities) {
}

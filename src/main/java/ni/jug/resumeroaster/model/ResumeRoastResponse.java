package ni.jug.resumeroaster.model;

import java.util.List;

/**
 * @author jxareas
 */
public record ResumeRoastResponse(String answer, List<EntityMention> entities) {
}

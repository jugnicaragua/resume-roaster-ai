package ni.jug.resumeroaster.model;

import java.util.List;

/**
 * @author jxareas
 */
public record NerResponse(String text, List<EntityMention> entities) {
}

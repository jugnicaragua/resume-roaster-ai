package ni.jug.resumeroaster.model;

import dev.langchain4j.service.TokenStream;

import java.util.List;

/**
 * @author jxareas
 */
public record StreamingRoastResult(TokenStream tokenStream, List<EntityMention> entities) {
}

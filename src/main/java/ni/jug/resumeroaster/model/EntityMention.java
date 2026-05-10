package ni.jug.resumeroaster.model;

/**
 * @author jxareas
 */
public record EntityMention(String text, String type, double confidence, NerSource source, int startOffset, int endOffset) {
}

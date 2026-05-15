package ni.jug.resumeroaster.model;

/**
 * @author jxareas
 */
public record EntityMention(String text, String type, double confidence, DetectionMethod detectionMethod, int startOffset, int endOffset) {
}

package ni.jug.resumeroaster.service;

/**
 * @author jxareas
 */
@FunctionalInterface
public interface TextNormalizer {

    String normalize(String text);
}

package ni.jug.resumeroaster.service;

import ni.jug.resumeroaster.model.EntityMention;

import java.util.List;

/**
 * Contract for rule-based detectors that extract {@link EntityMention}s via pattern matching.
 *
 * @author jxareas
 */
public interface PatternMatchingDetector {

    List<EntityMention> detect(String text);
}

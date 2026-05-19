package ni.jug.resumeroaster.service;

import ni.jug.resumeroaster.model.EntityMention;

import java.util.List;

/**
 * @author jxareas
 */
public interface TextRedactor {

    String redactText(String text, List<EntityMention> entities);
}

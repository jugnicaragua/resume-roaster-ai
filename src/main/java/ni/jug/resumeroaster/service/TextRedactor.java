package ni.jug.resumeroaster.service;

import ni.jug.resumeroaster.model.Redaction;

/**
 * @author jxareas
 */
public interface TextRedactor {

    Redaction redactText(String text);
}

package ni.jug.resumeroaster.service;

import ni.jug.resumeroaster.model.EntityMention;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author jxareas
 */
public interface TextProcessor {

    String extractAndNormalize(MultipartFile file);

    String redact(String text, List<EntityMention> entities);
}

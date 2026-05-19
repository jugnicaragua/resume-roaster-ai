package ni.jug.resumeroaster.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * @author jxareas
 */
@FunctionalInterface
public interface TextExtractor {
    String extract(MultipartFile file);
}

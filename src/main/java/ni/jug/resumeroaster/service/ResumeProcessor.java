package ni.jug.resumeroaster.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * @author jxareas
 */
public interface ResumeProcessor {

    String extractText(MultipartFile file);
}

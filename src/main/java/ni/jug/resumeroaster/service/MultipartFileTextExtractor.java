package ni.jug.resumeroaster.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author jxareas
 */
@Component
public class MultipartFileTextExtractor implements TextExtractor {

    private static final Tika TIKA = new Tika();

    @Override
    public String extractText(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return TIKA.parseToString(inputStream);
        } catch (TikaException | IOException e) {
            throw new RuntimeException("Failed to extract text from resume: " + file.getOriginalFilename(), e);
        }
    }
}

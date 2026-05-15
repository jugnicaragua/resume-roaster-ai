package ni.jug.resumeroaster.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author jxareas
 */
@Slf4j
@Component
public class MultipartFileTextExtractor implements TextExtractor {

    private static final Tika TIKA = new Tika();

    @Override
    public String extractText(MultipartFile file) {
        log.info("Extracting text from file: {}", file.getOriginalFilename());
        try (InputStream inputStream = file.getInputStream()) {
            String text = TIKA.parseToString(inputStream);
            log.debug("Extracted {} characters from {}", text.length(), file.getOriginalFilename());
            return text;
        } catch (TikaException | IOException e) {
            throw new RuntimeException("Failed to extract text from resume: " + file.getOriginalFilename(), e);
        }
    }
}

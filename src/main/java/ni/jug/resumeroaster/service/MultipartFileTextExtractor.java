package ni.jug.resumeroaster.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@link TextExtractor} implementation that extracts plain text from uploaded resume files.
 *
 * <p>Supports any file format accepted by the underlying parser (PDF, DOCX, ODT, plain text, etc.).
 *
 * @author jxareas
 * @see TextExtractor
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MultipartFileTextExtractor implements TextExtractor {

    private final Tika tika;

    /**
     * Extracts plain text from the given multipart file.
     *
     * @param file the uploaded resume file
     * @return the extracted plain text content
     * @throws RuntimeException if the file cannot be read or parsed
     */
    @Override
    public String extract(MultipartFile file) {
        log.info("Extracting text from file: {}", file.getOriginalFilename());
        try (InputStream inputStream = file.getInputStream()) {
            String text = tika.parseToString(inputStream);
            log.debug("Extracted {} characters from {}", text.length(), file.getOriginalFilename());
            return text;
        } catch (TikaException | IOException e) {
            throw new RuntimeException("Failed to extract text from resume: " + file.getOriginalFilename(), e);
        }
    }
}

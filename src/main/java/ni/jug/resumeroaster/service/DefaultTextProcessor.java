package ni.jug.resumeroaster.service;

import lombok.RequiredArgsConstructor;
import ni.jug.resumeroaster.model.EntityMention;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author jxareas
 */
@Service
@RequiredArgsConstructor
public class DefaultTextProcessor implements TextProcessor {

    private final TextExtractor textExtractor;
    private final TextNormalizer textNormalizer;
    private final TextRedactor textRedactor;

    @Override
    public String extractAndNormalize(MultipartFile file) {
        var extractedText = textExtractor.extract(file);
        return textNormalizer.normalize(extractedText);
    }

    @Override
    public String redact(String text, List<EntityMention> namedEntities) {
        return textRedactor.redact(text, namedEntities);
    }
}

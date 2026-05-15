package ni.jug.resumeroaster.service;

import dev.langchain4j.service.TokenStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import ni.jug.resumeroaster.ai.annotations.ClassicalNlpNer;
import ni.jug.resumeroaster.ai.model.NerModel;
import ni.jug.resumeroaster.config.CoreNlpConfigurationProperties;
import ni.jug.resumeroaster.model.EntityMention;
import ni.jug.resumeroaster.model.ResumeRoastResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Orchestrates the resume roasting pipeline: extract text → detect entities → redact PII → generate roast.
 *
 * @author jxareas
 */
@Service
@RequiredArgsConstructor
public class ResumeRoastService {

    private final TextExtractor textExtractor;
    private final TextRedactor textRedactor;
    private final RoastLlmService roastLlmService;
    private final CoreNlpConfigurationProperties corenlpConfig;

    @ClassicalNlpNer
    private final NerModel nerModel;

    public ResumeRoastResponse roastResume(MultipartFile resume) {
        String text = textExtractor.extractText(resume);
        var nerResponse = nerModel.infer(text);
        String redactedText = textRedactor.redactText(text).redactedText();
        String roast = roastLlmService.generateRoast(redactedText);
        List<EntityMention> filteredEntities = filterEntitiesByTargetTags(nerResponse.entities());
        return new ResumeRoastResponse(roast, filteredEntities);
    }

    public StreamingRoastWrapper roastResumeStream(MultipartFile resume) {
        String text = textExtractor.extractText(resume);
        var nerResponse = nerModel.infer(text);
        String redactedText = textRedactor.redactText(text).redactedText();
        TokenStream tokenStream = roastLlmService.generateRoastStream(redactedText);
        List<EntityMention> filteredEntities = filterEntitiesByTargetTags(nerResponse.entities());
        return new StreamingRoastWrapper(tokenStream, filteredEntities);
    }

    private List<EntityMention> filterEntitiesByTargetTags(List<EntityMention> entities) {
        return entities.stream()
                .filter(e -> corenlpConfig.getTargetTags().contains(e.type()))
                .distinct()
                .toList();
    }

    public record StreamingRoastWrapper(TokenStream tokenStream, List<EntityMention> entities) {
    }
}

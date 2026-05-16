package ni.jug.resumeroaster.service;

import dev.langchain4j.service.TokenStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ni.jug.resumeroaster.ai.annotations.ClassicalNlpNer;
import ni.jug.resumeroaster.ai.model.NerModel;
import ni.jug.resumeroaster.configuration.properties.CoreNlpConfigurationProperties;
import ni.jug.resumeroaster.model.EntityMention;
import ni.jug.resumeroaster.model.ResumeRoastResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Orchestrates the resume roasting pipeline: extract text → detect entities → redact PII → generate roast.
 *
 * @author jxareas
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeRoastService {

    private final TextExtractor textExtractor;
    private final TextRedactor textRedactor;
    private final RoastLlmService roastLlmService;
    private final RoastWithRedactionService roastWithRedactionService;
    private final CoreNlpConfigurationProperties corenlpConfig;

    @ClassicalNlpNer
    private final NerModel nerModel;

    public ResumeRoastResponse roastResume(MultipartFile resume) {
        log.info("Starting resume roast pipeline for file: {}", resume.getOriginalFilename());
        String text = textExtractor.extractText(resume);
        var nerResponse = nerModel.infer(text);
        log.debug("NER detected {} entities", nerResponse.entities().size());
        String redactedText = textRedactor.redactText(text).redactedText();
        log.debug("PII redaction complete");
        String roast = roastLlmService.generateRoast(redactedText);
        log.info("Roast generated successfully");
        List<EntityMention> filteredEntities = filterEntitiesByTargetTags(nerResponse.entities());
        return new ResumeRoastResponse(roast, filteredEntities);
    }

    public StreamingRoastWrapper roastResumeStream(MultipartFile resume) {
        log.info("Starting streaming resume roast pipeline for file: {}", resume.getOriginalFilename());
        String text = textExtractor.extractText(resume);
        var nerResponse = nerModel.infer(text);
        log.debug("NER detected {} entities", nerResponse.entities().size());
        String redactedText = textRedactor.redactText(text).redactedText();
        log.debug("PII redaction complete");
        TokenStream tokenStream = roastLlmService.generateRoastStream(redactedText);
        List<EntityMention> filteredEntities = filterEntitiesByTargetTags(nerResponse.entities());
        return new StreamingRoastWrapper(tokenStream, filteredEntities);
    }

    private List<EntityMention> filterEntitiesByTargetTags(List<EntityMention> entities) {
        return entities.stream()
                .filter(e -> corenlpConfig.getTargetTags().contains(e.type()))
                .toList();
    }

    public TokenStream roastResumeStreamWithTools(MultipartFile resume) {
        log.info("Starting tool-calling resume roast for: {}", resume.getOriginalFilename());
        String text = textExtractor.extractText(resume);
        return roastWithRedactionService.generateRoast(text);
    }

    public record StreamingRoastWrapper(TokenStream tokenStream, List<EntityMention> entities) {
    }
}

package ni.jug.resumeroaster.service;

import dev.langchain4j.service.TokenStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ni.jug.resumeroaster.model.EntityMention;
import ni.jug.resumeroaster.model.NerInferenceBackend;
import ni.jug.resumeroaster.model.ResumeRoastResponse;
import ni.jug.resumeroaster.service.ner.NerStrategyRegistry;
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
    private final TextNormalizer textNormalizer;
    private final TextRedactor textRedactor;
    private final RoastLlmService roastLlmService;
    private final NerStrategyRegistry nerStrategyRegistry;

    public ResumeRoastResponse roastResume(MultipartFile resume, NerInferenceBackend nerInferenceBackend) {
        log.info("Starting resume roast pipeline for file: {} (backend={})", resume.getOriginalFilename(), nerInferenceBackend);
        String text = textNormalizer.normalize(textExtractor.extractText(resume));
        List<EntityMention> entities = nerStrategyRegistry.get(nerInferenceBackend).detect(text);
        log.debug("NER detected {} entities", entities.size());
        String redactedText = textRedactor.redactText(text, entities);
        String roast = roastLlmService.generateRoast(redactedText);
        log.info("Roast generated successfully");
        return new ResumeRoastResponse(text, redactedText, roast, entities);
    }

    public StreamingRoastWrapper roastResumeStream(MultipartFile resume, NerInferenceBackend nerInferenceBackend) {
        log.info("Starting streaming resume roast pipeline for file: {} (backend={})", resume.getOriginalFilename(), nerInferenceBackend);
        String text = textNormalizer.normalize(textExtractor.extractText(resume));
        List<EntityMention> entities = nerStrategyRegistry.get(nerInferenceBackend).detect(text);
        log.debug("NER detected {} entities", entities.size());
        String redactedText = textRedactor.redactText(text, entities);
        TokenStream tokenStream = roastLlmService.generateRoastStream(redactedText);
        return new StreamingRoastWrapper(tokenStream, entities);
    }

    public record StreamingRoastWrapper(TokenStream tokenStream, List<EntityMention> entities) {
    }
}

package ni.jug.resumeroaster.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ni.jug.resumeroaster.model.EntityMention;
import ni.jug.resumeroaster.model.NerInferenceBackend;
import ni.jug.resumeroaster.model.ResumeRoastResponse;
import ni.jug.resumeroaster.model.StreamingRoastResult;
import ni.jug.resumeroaster.service.ner.NerStrategyRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Orchestrates the resume roasting pipeline: extract text → detect entities → redact PII → generate roast.
 *
 * @author jxareas
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeRoastService {

    private final TextProcessor textProcessor;
    private final NerStrategyRegistry nerStrategyRegistry;
    private final RoastLlmService roastLlmService;

    public ResumeRoastResponse roastResume(MultipartFile resume, NerInferenceBackend nerInferenceBackend) {
        log.info("Starting resume roast pipeline for file: {} (backend={})", resume.getOriginalFilename(), nerInferenceBackend);
        String text = textProcessor.extractAndNormalize(resume);
        List<EntityMention> entities = nerStrategyRegistry.get(nerInferenceBackend).detect(text);

        log.debug("NER detected {} entities", entities.size());
        String redactedText = textProcessor.redact(text, entities);
        String roast = roastLlmService.generateRoast(redactedText);

        log.info("Roast generated successfully");
        return new ResumeRoastResponse(text, redactedText, roast, entities);
    }

    public StreamingRoastResult roastResumeStream(MultipartFile resume, NerInferenceBackend nerInferenceBackend) {
        log.info("Starting streaming resume roast pipeline for file: {} (backend={})", resume.getOriginalFilename(), nerInferenceBackend);
        String text = textProcessor.extractAndNormalize(resume);
        List<EntityMention> entities = nerStrategyRegistry.get(nerInferenceBackend).detect(text);

        log.debug("NER detected {} entities", entities.size());
        String redactedText = textProcessor.redact(text, entities);

        return new StreamingRoastResult(roastLlmService.generateRoastStream(redactedText), entities);
    }
}

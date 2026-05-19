package ni.jug.resumeroaster.service;

import dev.langchain4j.service.TokenStream;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ni.jug.resumeroaster.ai.annotations.ClassicalNlpNer;
import ni.jug.resumeroaster.ai.annotations.NeuralNer;
import ni.jug.resumeroaster.ai.model.NerModel;
import ni.jug.resumeroaster.configuration.properties.CoreNlpConfigurationProperties;
import ni.jug.resumeroaster.model.EntityMention;
import ni.jug.resumeroaster.model.NerInferenceBackend;
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
    private final TextNormalizer textNormalizer;
    private final TextRedactor textRedactor;
    private final RoastLlmService roastLlmService;
    private final List<PatternMatchingDetector> patternDetectors;
    private final CoreNlpConfigurationProperties corenlpConfig;

    @ClassicalNlpNer
    private final NerModel corenlpNerModel;

    @NeuralNer
    private final NerModel djlNerModel;

    public ResumeRoastResponse roastResume(MultipartFile resume, NerInferenceBackend nerInferenceBackend) {
        log.info("Starting resume roast pipeline for file: {} (backend={})", resume.getOriginalFilename(), nerInferenceBackend);
        String text = textNormalizer.normalize(textExtractor.extractText(resume));
        List<EntityMention> entities = runNer(text, nerInferenceBackend);
        log.debug("NER detected {} entities", entities.size());
        String redactedText = textRedactor.redactText(text, entities);
        String roast = roastLlmService.generateRoast(redactedText);
        log.info("Roast generated successfully");
        return new ResumeRoastResponse(text, redactedText, roast, entities);
    }

    public StreamingRoastWrapper roastResumeStream(MultipartFile resume, NerInferenceBackend nerInferenceBackend) {
        log.info("Starting streaming resume roast pipeline for file: {} (backend={})", resume.getOriginalFilename(), nerInferenceBackend);
        String text = textNormalizer.normalize(textExtractor.extractText(resume));
        List<EntityMention> entities = runNer(text, nerInferenceBackend);
        log.debug("NER detected {} entities", entities.size());
        String redactedText = textRedactor.redactText(text, entities);
        TokenStream tokenStream = roastLlmService.generateRoastStream(redactedText);
        return new StreamingRoastWrapper(tokenStream, entities);
    }

    private List<EntityMention> runNer(String text, NerInferenceBackend nerInferenceBackend) {
        if (nerInferenceBackend == NerInferenceBackend.DJL_REGEX) {
            return inferHybrid(text);
        }
        List<EntityMention> entities = selectNerModel(nerInferenceBackend).inferChunked(text).entities();
        if (nerInferenceBackend == NerInferenceBackend.CORENLP) {
            return entities.stream()
                    .filter(e -> corenlpConfig.getTargetTags().contains(e.type()))
                    .toList();
        }
        return entities;
    }

    private List<EntityMention> inferHybrid(String text) {
        // TODO: Add Transformer-based configurable target tags for NER inference
        List<EntityMention> persons = djlNerModel.inferChunked(text).entities().stream()
                .filter(e -> "PER".equals(e.type()) || "LOC".equals(e.type()))
                .toList();

        List<EntityMention> patternDetected = patternDetectors.stream()
                .flatMap(d -> d.detect(text).stream())
                .toList();

        List<EntityMention> merged = new ArrayList<>(persons.size() + patternDetected.size());
        merged.addAll(persons);
        merged.addAll(patternDetected);
        log.debug("Hybrid NER: {} PER/LOC from DJL + {} from pattern detectors", persons.size(), patternDetected.size());
        return EntityMention.deduplicate(merged);
    }

    private NerModel selectNerModel(NerInferenceBackend nerInferenceBackend) {
        return nerInferenceBackend == NerInferenceBackend.DJL ? djlNerModel : corenlpNerModel;
    }

    public record StreamingRoastWrapper(TokenStream tokenStream, List<EntityMention> entities) {
    }
}

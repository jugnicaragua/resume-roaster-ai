package ni.jug.resumeroaster.ai.tool;

import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ni.jug.resumeroaster.ai.annotations.ClassicalNlpNer;
import ni.jug.resumeroaster.ai.model.NerModel;
import ni.jug.resumeroaster.configuration.properties.CoreNlpConfigurationProperties;
import ni.jug.resumeroaster.model.EntityMention;
import ni.jug.resumeroaster.model.PiiRedactionResult;
import ni.jug.resumeroaster.service.TextRedactor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * LangChain4J tool that detects and redacts PII from raw resume text.
 *
 * <p>Runs CoreNLP NER to identify entities, applies text redaction, and returns a
 * {@link PiiRedactionResult} serialized as JSON so the LLM can use the sanitized text.
 *
 * @author jxareas
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PiiRedactionTool {

    @ClassicalNlpNer
    private final NerModel nerModel;
    private final TextRedactor textRedactor;
    private final CoreNlpConfigurationProperties corenlpConfig;
    private final ObjectMapper objectMapper;

    @Tool("Detects and redacts personally identifiable information from resume text. Returns a JSON object with 'redactedText' (sanitized resume) and 'entities' (detected PII entities).")
    public String redactPii(String resumeText) {
        log.debug("PII redaction tool invoked on {} characters", resumeText.length());
        var nerResponse = nerModel.infer(resumeText);
        var redaction = textRedactor.redactText(resumeText);
        List<EntityMention> entities = nerResponse.entities().stream()
                .filter(e -> corenlpConfig.getTargetTags().contains(e.type()))
                .toList();
        log.debug("Redacted {} PII entities", entities.size());
        try {
            return objectMapper.writeValueAsString(new PiiRedactionResult(redaction.redactedText(), entities));
        } catch (Exception e) {
            log.error("Failed to serialize PII redaction result", e);
            return redaction.redactedText();
        }
    }
}

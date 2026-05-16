package ni.jug.resumeroaster.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ni.jug.resumeroaster.ai.annotations.ClassicalNlpNer;
import ni.jug.resumeroaster.ai.annotations.NeuralNer;
import ni.jug.resumeroaster.ai.model.NerModel;
import ni.jug.resumeroaster.model.NerRequest;
import ni.jug.resumeroaster.model.NerResponse;
import ni.jug.resumeroaster.model.NerSortField;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

/**
 * REST controller exposing NER inference over raw text via two independent backends.
 *
 * <p>Both endpoints accept a plain-text body, run the requested NER backend, deduplicate
 * the detected spans, and return them sorted by the chosen {@link NerSortField}.
 *
 * <ul>
 *   <li>{@code POST /api/ner/entities} — Stanford CoreNLP (CRF + rule-based patterns)</li>
 *   <li>{@code POST /api/ner/entities/djl} — DistilBERT ONNX via DJL</li>
 * </ul>
 *
 * @author jxareas
 */
@RestController
@RequestMapping("/api/ner")
@RequiredArgsConstructor
@Tag(name = "NER Inference", description = "Named Entity Recognition endpoints for detecting and classifying entities in text.")
public class NerInferenceController {

    @ClassicalNlpNer
    private final NerModel classicalNer;

    @NeuralNer
    private final NerModel neuralNer;

    /**
     * Runs CoreNLP NER on the given text and returns deduplicated entity mentions.
     *
     * @param request the text to run inference on
     * @param sortBy  field to sort results by; defaults to {@link NerSortField#CONFIDENCE}
     * @return detected entities sorted by the requested field
     */
    @Operation(summary = "Detect Entities", description = "Run NER inference on a given text and return detected entities with their type and confidence score.")
    @PostMapping("/entities")
    public ResponseEntity<NerResponse> detectEntities(
            @Valid @RequestBody NerRequest request,
            @RequestParam(defaultValue = "CONFIDENCE") NerSortField sortBy) {
        var sortedEntities = inferSorted(classicalNer, request.text(), sortBy);
        return ResponseEntity.ok(sortedEntities);
    }

    /**
     * Runs DistilBERT ONNX NER via DJL on the given text and returns deduplicated entity mentions.
     *
     * @param request the text to run inference on
     * @param sortBy  field to sort results by; defaults to {@link NerSortField#CONFIDENCE}
     * @return detected entities sorted by the requested field
     */
    @Operation(summary = "Detect Entities (DJL)", description = "Run NER inference using the ONNX DistilBERT model via DJL and return detected entities with their type and confidence score.")
    @PostMapping("/entities/djl")
    public ResponseEntity<NerResponse> detectEntitiesDjl(
            @Valid @RequestBody NerRequest request,
            @RequestParam(defaultValue = "CONFIDENCE") NerSortField sortBy) {
        var sortedEntities = inferSorted(neuralNer, request.text(), sortBy);
        return ResponseEntity.ok(sortedEntities);
    }

    /** Runs inference on {@code text} with a given NER model and returns the result sorted by {@code sortBy}. */
    private static NerResponse inferSorted(NerModel nerModel, String text, NerSortField sortBy) {
        var inferredEntities = nerModel.infer(text);
        return sorted(inferredEntities, sortBy);
    }

    /** Returns a copy of {@code response} with its entities sorted according to {@code sortBy}. */
    private static NerResponse sorted(NerResponse response, NerSortField sortBy) {
        var entities = new ArrayList<>(response.entities());
        entities.sort(sortBy.comparator());
        return new NerResponse(response.text(), entities);
    }
}

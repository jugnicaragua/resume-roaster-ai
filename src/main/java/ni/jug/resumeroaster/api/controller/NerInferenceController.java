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
import ni.jug.resumeroaster.service.TextExtractor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

/**
 * @author jxareas
 */
@RestController
@RequestMapping("/api/ner")
@RequiredArgsConstructor
@Tag(name = "NER Inference", description = "Named Entity Recognition endpoints for detecting and classifying entities in text.")
public class NerInferenceController {

    @ClassicalNlpNer
    private final NerModel classicalNerModel;

    @NeuralNer
    private final NerModel neuralNerModel;

    @Operation(summary = "Detect Entities", description = "Run NER inference on a given text and return detected entities with their type and confidence score.")
    @PostMapping("/entities")
    public ResponseEntity<NerResponse> detectEntities(
            @Valid @RequestBody NerRequest request,
            @RequestParam(defaultValue = "START_OFFSET") NerSortField sortBy) {
        return ResponseEntity.ok(sorted(classicalNerModel.infer(request.text()), sortBy));
    }

    @Operation(summary = "Detect Entities (DJL)", description = "Run NER inference using the ONNX DistilBERT model via DJL and return detected entities with their type and confidence score.")
    @PostMapping("/entities/djl")
    public ResponseEntity<NerResponse> detectEntitiesDjl(
            @Valid @RequestBody NerRequest request,
            @RequestParam(defaultValue = "START_OFFSET") NerSortField sortBy) {
        return ResponseEntity.ok(sorted(neuralNerModel.infer(request.text()), sortBy));
    }

    private static NerResponse sorted(NerResponse response, NerSortField sortBy) {
        var entities = new ArrayList<>(response.entities());
        entities.sort(sortBy.comparator());
        return new NerResponse(response.text(), entities);
    }
}

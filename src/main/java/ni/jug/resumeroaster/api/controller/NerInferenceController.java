package ni.jug.resumeroaster.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ni.jug.resumeroaster.model.NerRequest;
import ni.jug.resumeroaster.model.NerResponse;
import ni.jug.resumeroaster.service.NerInferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jxareas
 */
@RestController
@RequestMapping("/api/ner")
@Tag(name = "NER - Named Entity Recognition", description = "Named Entity Recognition endpoints for detecting and classifying entities in text.")
public class NerInferenceController {

    private final NerInferenceService nerInferenceService;

    public NerInferenceController(NerInferenceService nerInferenceService) {
        this.nerInferenceService = nerInferenceService;
    }

    @Operation(summary = "Detect Entities", description = "Run NER inference on a given text and return detected entities with their type and confidence score.")
    @PostMapping("/entities")
    public ResponseEntity<NerResponse> detectEntities(@Valid @RequestBody NerRequest request) {
        return ResponseEntity.ok(nerInferenceService.infer(request.text()));
    }
}

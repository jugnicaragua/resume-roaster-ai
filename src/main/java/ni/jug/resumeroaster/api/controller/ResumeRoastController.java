package ni.jug.resumeroaster.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ni.jug.resumeroaster.model.ChatCompletionChunk;
import ni.jug.resumeroaster.model.NerInferenceBackend;
import ni.jug.resumeroaster.model.ResumeRoastResponse;
import ni.jug.resumeroaster.service.ResumeRoastService;
import ni.jug.resumeroaster.util.SseSupport;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

/**
 * @author jxareas
 */
@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
@Tag(name = "Resume Roasting", description = "Resume roasting endpoints that extract text, detect entities, redact PII, and generate AI-powered feedback.")
public class ResumeRoastController {

    private final ResumeRoastService resumeRoastService;

    @Operation(summary = "Roast Resume", description = "Upload a resume file, detect entities, redact PII, and generate a roast using the LLM.")
    @PostMapping(value = "/roast", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeRoastResponse> roastResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "DJL_REGEX") NerInferenceBackend nerInferenceBackend) {
        ResumeRoastResponse response = resumeRoastService.roastResume(file, nerInferenceBackend);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Roast Resume Stream", description = "Upload a resume file, redact PII, and stream the roast via Server-Sent Events.")
    @PostMapping(value = "/roast/stream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter roastResumeStream(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "DJL_REGEX") NerInferenceBackend nerInferenceBackend) {
        SseEmitter emitter = SseSupport.createEmitter(60_000L);
        String id = ChatCompletionChunk.generateId();

        try {
            var wrapper = resumeRoastService.roastResumeStream(file, nerInferenceBackend);
            emitEntities(emitter, wrapper.entities());
            wrapper.tokenStream()
                    .onPartialResponse(chunk -> SseSupport.emitChunk(emitter, ChatCompletionChunk.token(id, chunk)))
                    .onError(emitter::completeWithError)
                    .onCompleteResponse(response -> {
                        SseSupport.emitChunk(emitter, ChatCompletionChunk.stop(id));
                        SseSupport.emitDone(emitter);
                        emitter.complete();
                    })
                    .start();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    private void emitEntities(SseEmitter emitter, List<?> entities) {
        try {
            emitter.send(SseEmitter.event().name("entities").data(entities, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            throw new RuntimeException("Failed to emit entities", e);
        }
    }
}

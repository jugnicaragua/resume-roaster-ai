package ni.jug.resumeroaster.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ni.jug.resumeroaster.model.ResumeRoastResponse;
import ni.jug.resumeroaster.service.ResumeRoastService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

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
    public ResponseEntity<ResumeRoastResponse> roastResume(@RequestParam("file") MultipartFile file) {
        ResumeRoastResponse response = resumeRoastService.roastResume(file);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Roast Resume Stream", description = "Upload a resume file, detect entities, redact PII, and generate a roast using the LLM with streaming response (Server-Sent Events).")
    @PostMapping(value = "/roast/stream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter roastResumeStream(@RequestParam("file") MultipartFile file) {
        SseEmitter emitter = new SseEmitter(30_000L);
        emitter.onTimeout(emitter::complete);

        try {
            var wrapper = resumeRoastService.roastResumeStream(file);
            emitEntities(emitter, wrapper.entities());
            wrapper.tokenStream()
                    .onPartialResponse(chunk -> emit(emitter, chunk))
                    .onError(emitter::completeWithError)
                    .onCompleteResponse(response -> emitter.complete())
                    .start();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    private void emitEntities(SseEmitter emitter, java.util.List<?> entities) {
        try {
            emitter.send(SseEmitter.event().id("entities").data(entities));
        } catch (IOException e) {
            throw new RuntimeException("Failed to emit entities", e);
        }
    }

    private void emit(SseEmitter emitter, String chunk) {
        try {
            emitter.send(SseEmitter.event().data(chunk));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}

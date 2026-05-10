package ni.jug.resumeroaster.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ni.jug.resumeroaster.model.ChatRequest;
import ni.jug.resumeroaster.model.ChatResponse;
import ni.jug.resumeroaster.service.ChatService;
import java.io.IOException;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author jxareas
 */
@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "LLM chat endpoints for prompt-based text generation.")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(summary = "Generate Response", description = "Run LLM inference on a given user prompt.")
    @PostMapping("/response")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String response = chatService.chat(request.message());
        return ResponseEntity.ok(new ChatResponse(response));
    }

    @Operation(summary = "Generate Response Stream", description = "Run LLM inference with streaming response (Server-Sent Events).")
    @PostMapping(value = "/response/stream", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(30_000L);
        emitter.onTimeout(emitter::complete);

        chatService.streamChat(request.message())
                .onPartialResponse(chunk -> emit(emitter, chunk))
                .onError(emitter::completeWithError)
                .onCompleteResponse(response -> emitter.complete())
                .start();

        return emitter;
    }

    private void emit(SseEmitter emitter, String chunk) {
        try {
            emitter.send(SseEmitter.event().data(chunk));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}

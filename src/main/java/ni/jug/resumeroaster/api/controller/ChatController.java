package ni.jug.resumeroaster.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ni.jug.resumeroaster.model.ChatCompletionChunk;
import ni.jug.resumeroaster.model.ChatRequest;
import ni.jug.resumeroaster.model.ChatResponse;
import ni.jug.resumeroaster.service.ChatService;
import ni.jug.resumeroaster.util.SseSupport;
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
@RequiredArgsConstructor
@Tag(name = "LLM Chat", description = "LLM chat endpoints for prompt-based text generation.")
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "Generate Response", description = "Run LLM inference on a given user prompt.")
    @PostMapping("/response")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String response = chatService.chat(request.message());
        return ResponseEntity.ok(new ChatResponse(response));
    }

    @Operation(summary = "Generate Response Stream", description = "Run LLM inference with streaming response (Server-Sent Events).")
    @PostMapping(value = "/response/stream", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = SseSupport.createEmitter(30_000L);
        String id = ChatCompletionChunk.generateId();

        chatService.streamChat(request.message())
                .onPartialResponse(chunk -> SseSupport.emitChunk(emitter, ChatCompletionChunk.token(id, chunk)))
                .onError(emitter::completeWithError)
                .onCompleteResponse(response -> {
                    SseSupport.emitChunk(emitter, ChatCompletionChunk.stop(id));
                    SseSupport.emitDone(emitter);
                    emitter.complete();
                })
                .start();

        return emitter;
    }
}

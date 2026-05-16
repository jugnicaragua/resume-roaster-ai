package ni.jug.resumeroaster.util;

import ni.jug.resumeroaster.model.ChatCompletionChunk;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * @author jxareas
 */
public final class SseSupport {

    private SseSupport() {}

    public static SseEmitter createEmitter(long timeoutMillis) {
        SseEmitter emitter = new SseEmitter(timeoutMillis);
        emitter.onTimeout(emitter::complete);
        return emitter;
    }

    public static void emitChunk(SseEmitter emitter, ChatCompletionChunk chunk) {
        try {
            emitter.send(SseEmitter.event().data(chunk, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            emitter.completeWithError(e);
        } catch (IllegalStateException ignored) {
            // Client disconnected before the emitter completed — nothing to do
        }
    }

    public static void emitDone(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().data("[DONE]"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        } catch (IllegalStateException ignored) {
            // Client disconnected before the emitter completed — nothing to do
        }
    }
}

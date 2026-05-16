package ni.jug.resumeroaster.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

/**
 * @author jxareas
 */
public record ChatCompletionChunk(String id, String object, List<Choice> choices) {

    public record Choice(
            int index,
            Delta delta,
            @JsonProperty("finish_reason") String finishReason) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Delta(String content) {}

    public static ChatCompletionChunk token(String id, String content) {
        return new ChatCompletionChunk(id, "chat.completion.chunk",
                List.of(new Choice(0, new Delta(content), null)));
    }

    public static ChatCompletionChunk stop(String id) {
        return new ChatCompletionChunk(id, "chat.completion.chunk",
                List.of(new Choice(0, new Delta(null), "stop")));
    }

    public static String generateId() {
        return "chatcmpl-" + UUID.randomUUID().toString().replace("-", "");
    }
}

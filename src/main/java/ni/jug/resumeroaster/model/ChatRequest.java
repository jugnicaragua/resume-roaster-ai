package ni.jug.resumeroaster.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @author jxareas
 */
public record ChatRequest(
        @NotBlank(message = "Message is required")
        @Size(max = 10_000, message = "Message is too long")
        String message) {
}

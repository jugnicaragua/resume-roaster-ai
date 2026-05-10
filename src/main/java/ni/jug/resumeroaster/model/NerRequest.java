package ni.jug.resumeroaster.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @author jxareas
 */
public record NerRequest(
        @NotBlank(message = "Text is required")
        @Size(max = 10_000, message = "Text is too long")
        String text) {
}

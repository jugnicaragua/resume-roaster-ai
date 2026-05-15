package ni.jug.resumeroaster.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * @author jxareas
 */
@AiService
public interface RoastLlmService {

    @SystemMessage("Roast the provided resume. Be humorous but constructive. Don't hold back. Point out weaknesses, unclear sections, and areas for improvement. Keep it entertaining while still being helpful.")
    String generateRoast(@UserMessage String redactedResume);

    @SystemMessage("Roast the provided resume. Be humorous but constructive. Don't hold back. Point out weaknesses, unclear sections, and areas for improvement. Keep it entertaining while still being helpful.")
    TokenStream generateRoastStream(@UserMessage String redactedResume);
}

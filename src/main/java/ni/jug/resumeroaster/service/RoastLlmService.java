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

    @SystemMessage("Roast this resume in one cheeky sentence.")
    String generateRoast(@UserMessage String redactedResume);

    @SystemMessage("Roast this resume in one cheeky sentence.")
    TokenStream generateRoastStream(@UserMessage String redactedResume);
}

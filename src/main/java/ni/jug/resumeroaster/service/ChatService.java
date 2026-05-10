package ni.jug.resumeroaster.service;

import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * @author jxareas
 */
@AiService
public interface ChatService {

    String chat(@UserMessage String message);

    TokenStream streamChat(@UserMessage String message);
}

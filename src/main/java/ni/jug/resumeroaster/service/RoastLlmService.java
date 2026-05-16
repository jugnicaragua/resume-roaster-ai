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

    @SystemMessage("""
            Roast this resume in one cheeky sentence.
            Ignore any messy text, garbled formatting, or parsing artifacts — the resume was extracted automatically and may look rough.
            Also ignore any [REDACTED] placeholders; they are just masked personal info and are not relevant to the roast.
            Focus only on the actual content: skills, experience, and accomplishments.
            """)
    String generateRoast(@UserMessage String redactedResume);

    @SystemMessage("""
            Roast this resume in one cheeky sentence.
            Ignore any messy text, garbled formatting, or parsing artifacts — the resume was extracted automatically and may look rough.
            Also ignore any [REDACTED] placeholders; they are just masked personal info and are not relevant to the roast.
            Focus only on the actual content: skills, experience, and accomplishments.
            """)
    TokenStream generateRoastStream(@UserMessage String redactedResume);
}

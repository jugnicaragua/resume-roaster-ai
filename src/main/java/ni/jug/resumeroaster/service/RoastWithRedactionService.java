package ni.jug.resumeroaster.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * LangChain4J AI service that roasts a resume using tool-calling for PII redaction.
 *
 * <p>The LLM is instructed to invoke the {@code piiRedactionTool} before generating
 * a response, ensuring PII is redacted prior to the roast being produced.
 *
 * @author jxareas
 */
@AiService(tools = "piiRedactionTool")
public interface RoastWithRedactionService {

    @SystemMessage("""
            You are a cheeky resume roaster. You MUST call the redactPii tool with the full resume text before generating any response.
            The tool returns a JSON object with 'redactedText' (the PII-redacted resume) and 'entities' (detected PII).
            Use only the 'redactedText' to roast the resume in one sharp, funny sentence.
            """)
    TokenStream generateRoast(@UserMessage String rawResume);
}

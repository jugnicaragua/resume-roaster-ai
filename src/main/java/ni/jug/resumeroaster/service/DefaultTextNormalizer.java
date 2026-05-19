package ni.jug.resumeroaster.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;

/**
 * Cleans raw text extracted from documents (PDF, DOCX, etc.) before it is fed
 * into NER, PII redaction, or the LLM.
 *
 * @author jxareas
 */
@Service
public class DefaultTextNormalizer implements TextNormalizer {

    /**
     * Applies a sequential pipeline of normalization passes to {@code text}:
     * <ol>
     *   <li>Normalize line endings to {@code \n}</li>
     *   <li>Strip non-printable control characters (keeps {@code \n} and {@code \t})</li>
     *   <li>Apply Unicode NFC normalization</li>
     *   <li>Collapse runs of 3+ spaces within a line to a single space</li>
     *   <li>Trim leading/trailing whitespace from each line</li>
     *   <li>Collapse runs of 3+ consecutive blank lines to a double newline</li>
     *   <li>Trim the result</li>
     * </ol>
     *
     * @param text raw extracted text, may be null
     * @return normalized text, or an empty string if {@code text} is null or blank
     */
    @Override
    public String normalize(String text) {
        if (text == null || text.isBlank()) return "";

        String result = text
                .replace("\r\n", "\n")
                .replace("\r", "\n");

        result = stripControlCharacters(result);
        result = Normalizer.normalize(result, Normalizer.Form.NFC);
        result = collapseInlineWhitespace(result);
        result = trimLines(result);
        result = collapseBlankLines(result);

        return result.strip();
    }

    private static String stripControlCharacters(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (c == '\n' || c == '\t' || c >= 0x20) sb.append(c);
        }
        return sb.toString();
    }

    private static String collapseInlineWhitespace(String text) {
        return text.replaceAll("[ \t]{3,}", " ");
    }

    private static String trimLines(String text) {
        String[] lines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < lines.length; i++) {
            sb.append(lines[i].strip());
            if (i < lines.length - 1) sb.append('\n');
        }
        return sb.toString();
    }

    private static String collapseBlankLines(String text) {
        return text.replaceAll("\n{3,}", "\n\n");
    }
}

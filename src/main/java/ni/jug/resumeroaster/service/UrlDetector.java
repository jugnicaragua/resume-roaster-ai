package ni.jug.resumeroaster.service;

import lombok.extern.slf4j.Slf4j;
import ni.jug.resumeroaster.model.EntityMention;
import ni.jug.resumeroaster.model.EntityRecognitionMethod;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects URLs in free text using regex, covering both protocol-prefixed and bare URLs.
 *
 * @author jxareas
 */
@Slf4j
@Service
public class UrlDetector implements PatternMatchingDetector {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?:https?://|mailto:|www\\.)[^\\s,;)\"'<>]+" +
            "|(?:[a-zA-Z0-9][a-zA-Z0-9\\-]*\\.)+(?:com|io|org|net|dev|co|edu|gov|me|app|ai|tech)/[^\\s,;)\"'<>]*",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Scans {@code text} for URLs and returns each match as an {@link EntityMention}.
     *
     * @param text normalized input text
     * @return deduplicated list of detected URL mentions
     */
    public List<EntityMention> detect(String text) {
        List<EntityMention> results = new ArrayList<>();
        Matcher matcher = URL_PATTERN.matcher(text);
        while (matcher.find()) {
            results.add(new EntityMention(
                    matcher.group(),
                    "URL",
                    0.0,
                    1,
                    EntityRecognitionMethod.PATTERN_MATCHING
            ));
        }
        log.debug("URL detector found {} URLs", results.size());
        return EntityMention.deduplicate(results);
    }
}

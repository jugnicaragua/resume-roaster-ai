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
 * Detects email addresses in free text using regex.
 *
 * @author jxareas
 */
@Slf4j
@Service
public class EmailDetector implements PatternMatchingDetector {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Scans {@code text} for email addresses and returns each match as an {@link EntityMention}.
     *
     * @param text normalized input text
     * @return deduplicated list of detected email mentions
     */
    @Override
    public List<EntityMention> detect(String text) {
        List<EntityMention> results = new ArrayList<>();
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        while (matcher.find()) {
            results.add(new EntityMention(
                    matcher.group(),
                    "EMAIL",
                    0.0,
                    1,
                    EntityRecognitionMethod.PATTERN_MATCHING
            ));
        }
        log.debug("Email detector found {} emails", results.size());
        return EntityMention.deduplicate(results);
    }
}

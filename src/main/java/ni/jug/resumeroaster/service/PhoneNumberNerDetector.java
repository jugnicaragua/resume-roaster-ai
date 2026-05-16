package ni.jug.resumeroaster.service;

import com.google.i18n.phonenumbers.PhoneNumberMatch;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import lombok.extern.slf4j.Slf4j;
import ni.jug.resumeroaster.model.EntityMention;
import ni.jug.resumeroaster.model.EntityRecognitionMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects phone numbers in free text using Google's libphonenumber {@link PhoneNumberUtil}.
 *
 * @author jxareas
 */
@Slf4j
@Service
public class PhoneNumberNerDetector {

    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();

    private final String defaultRegion;

    public PhoneNumberNerDetector(@Value("${ner.phone.default-region:US}") String defaultRegion) {
        this.defaultRegion = defaultRegion;
    }

    /**
     * Scans {@code text} for phone numbers and returns each match as an {@link EntityMention}.
     *
     * @param text normalized input text
     * @return deduplicated list of detected phone number mentions
     */
    public List<EntityMention> detect(String text) {
        List<EntityMention> results = new ArrayList<>();
        for (PhoneNumberMatch match : PHONE_UTIL.findNumbers(text, defaultRegion, PhoneNumberUtil.Leniency.VALID, Long.MAX_VALUE)) {
            results.add(new EntityMention(
                    match.rawString(),
                    "PHONE_NUMBER",
                    0.0,
                    1,
                    EntityRecognitionMethod.PATTERN_MATCHING
            ));
        }
        log.debug("Phone detector found {} phone numbers", results.size());
        return EntityMention.deduplicate(results);
    }
}

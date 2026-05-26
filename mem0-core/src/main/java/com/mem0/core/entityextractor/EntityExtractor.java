package com.mem0.core.entityextractor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for extracting named entities from text.
 * Uses pattern-based extraction for common entity types.
 *
 * @author MoBai

 */
@Service
public class EntityExtractor {

    private static final Logger log = LoggerFactory.getLogger(EntityExtractor.class);

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");

    private static final Pattern URL_PATTERN =
        Pattern.compile("\\bhttps?://[\\S]+\\b");

    private static final Pattern PHONE_PATTERN =
        Pattern.compile("\\b\\+?\\d{1,3}[-.\\s]?\\(?\\d{1,4}\\)?[-.\\s]?\\d{1,4}[-.\\s]?\\d{1,9}\\b");

    private static final Pattern CAPITALIZED_PATTERN =
        Pattern.compile("\\b[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)+\\b");

    private static final Pattern HASHTAG_PATTERN =
        Pattern.compile("#[\\w]+");

    private static final Pattern MENTION_PATTERN =
        Pattern.compile("@[\\w]+");

    /**
     * Extracts named entities from the given text.
     *
     * @param text the text to extract entities from
     * @return the list of extracted entities
     */
    public List<String> extract(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        Set<String> entities = new HashSet<>();

        // Email addresses
        addMatches(entities, EMAIL_PATTERN.matcher(text));

        // URLs
        addMatches(entities, URL_PATTERN.matcher(text));

        // Phone numbers
        addMatches(entities, PHONE_PATTERN.matcher(text));

        // Capitalized phrases (likely names, places, organizations)
        addMatches(entities, CAPITALIZED_PATTERN.matcher(text));

        // Hashtags
        addMatches(entities, HASHTAG_PATTERN.matcher(text));

        // Mentions
        addMatches(entities, MENTION_PATTERN.matcher(text));

        List<String> result = new ArrayList<>(entities);
        log.debug("Extracted {} entities from text", result.size());
        return result;
    }

    private void addMatches(Set<String> entities, Matcher matcher) {
        while (matcher.find()) {
            String match = matcher.group().trim();
            if (!match.isEmpty()) {
                entities.add(match);
            }
        }
    }
}

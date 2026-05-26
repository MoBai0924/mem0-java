package com.mem0.core.utils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Advanced entity extraction utility, ported from Python mem0/utils/entity_extraction.py.
 * Extracts PROPER, QUOTED, COMPOUND, and NOUN entities from text.
 *
 * @author MoBai

 */
public final class EntityExtractorUtil {

    private EntityExtractorUtil() {}

    // Generic entity heads to filter out
    private static final Set<String> GENERIC_HEADS = Set.of(
        "thing", "stuff", "way", "time", "part", "place", "point",
        "case", "work", "job", "area", "field", "side", "line",
        "end", "bit", "piece", "kind", "sort", "type", "form",
        "step", "level", "stage", "phase", "section", "aspect",
        "factor", "element", "item", "issue", "problem", "question",
        "reason", "result", "effect", "difference", "change", "process"
    );

    // Generic capitalized single words to filter
    private static final Set<String> GENERIC_CAPS = Set.of(
        "The", "A", "An", "This", "That", "These", "Those",
        "My", "Your", "His", "Her", "Its", "Our", "Their",
        "I", "You", "He", "She", "It", "We", "They",
        "And", "Or", "But", "Not", "If", "Then", "When", "Where",
        "Yes", "No", "OK", "Okay", "Sure", "Right", "Well",
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
        "Saturday", "Sunday", "January", "February", "March",
        "April", "May", "June", "July", "August", "September",
        "October", "November", "December"
    );

    // Non-specific adjectives
    private static final Set<String> NON_SPECIFIC_ADJ = Set.of(
        "many", "few", "good", "bad", "big", "small", "old", "new",
        "long", "short", "high", "low", "great", "little", "large",
        "different", "same", "important", "possible", "certain",
        "general", "special", "simple", "common", "usual", "normal",
        "basic", "main", "major", "minor", "whole", "other", "next",
        "last", "first", "second", "third", "final", "previous",
        "recent", "current", "original", "local", "public", "private"
    );

    // Patterns
    private static final Pattern PROPER_PATTERN = Pattern.compile(
        "\\b([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)+)\\b"
    );
    private static final Pattern QUOTED_PATTERN = Pattern.compile(
        "\"([^\"]+)\"|'([^']+)'"
    );
    private static final Pattern ACRONYM_PATTERN = Pattern.compile(
        "\\b([A-Z]{2,6})\\b"
    );
    private static final Pattern COMPOUND_NP_PATTERN = Pattern.compile(
        "\\b((?:(?:[a-z]+\\s+)?(?:[A-Z][a-z]*\\s+)?[a-z]+\\s+){0,2}[a-z]+(?:ing|tion|ment|ness|ity|ance|ence|ism|ist|ory|ary|ery|iry|ure|age|dom|ship|hood|lat|ing|ive|al|ic|ous|ful|less|able|ible|ent|ant))\\b"
    );

    /**
     * Extracts entities from text, returning a list of unique entity strings.
     * Priority: PROPER > QUOTED > ACRONYM > COMPOUND
     *
     * @param text the input text
     * @return list of extracted entity strings
     */
    public static List<String> extractEntities(String text) {
        if (text == null || text.isBlank()) return List.of();

        Set<String> entities = new LinkedHashSet<>();

        // 1. PROPER entities (capitalized multi-word sequences)
        var properMatcher = PROPER_PATTERN.matcher(text);
        while (properMatcher.find()) {
            String entity = properMatcher.group(1).trim();
            if (!GENERIC_CAPS.contains(entity) && entity.length() > 2) {
                entities.add(entity);
            }
        }

        // 2. QUOTED entities (text in quotes)
        var quotedMatcher = QUOTED_PATTERN.matcher(text);
        while (quotedMatcher.find()) {
            String entity = (quotedMatcher.group(1) != null ? quotedMatcher.group(1) : quotedMatcher.group(2)).trim();
            if (entity.length() > 1) {
                entities.add(entity);
            }
        }

        // 3. ACRONYMS (2-6 uppercase letters)
        var acronymMatcher = ACRONYM_PATTERN.matcher(text);
        while (acronymMatcher.find()) {
            String entity = acronymMatcher.group(1).trim();
            if (!Set.of("THE", "AND", "FOR", "NOT", "BUT", "ARE", "WAS", "HAS", "HAD", "ITS", "WHO", "HOW", "WHY", "YES", "ALL", "CAN", "OUR", "YOU", "OUT", "USE", "GET", "LET", "SET", "PUT", "MAY", "OWN", "VIA", "PER", "ANY", "MIX", "TOP", "NEW", "OLD", "BIG", "KEY", "APP", "API", "URL", "SQL", "XML", "JSON", "HTML", "CSS", "HTTP", "REST", "SOAP", "TCP", "UDP", "SSH", "SSL", "VPN", "DNS", "CPU", "GPU", "RAM", "ROM", "IOS", "SDK", "IDE", "PDF", "CSV", "PNG", "JPG", "GIF", "MP3", "MP4", "DOC", "ZIP", "TAR", "GIT", "NPM", "PIP", "SSH", "AWS", "GCP", "IBM", "SRE", "ML", "AI", "DL", "NLP", "LLM", "RAG", "RAG", "ETL", "OLTP", "OLAP").contains(entity)) {
                entities.add(entity);
            }
        }

        // 4. COMPOUND noun phrases
        var compoundMatcher = COMPOUND_NP_PATTERN.matcher(text);
        while (compoundMatcher.find()) {
            String entity = compoundMatcher.group(1).trim().toLowerCase();
            String[] words = entity.split("\\s+");
            if (words.length >= 2 && !GENERIC_HEADS.contains(words[words.length - 1])) {
                boolean hasSpecificAdj = Arrays.stream(words)
                    .noneMatch(NON_SPECIFIC_ADJ::contains);
                if (hasSpecificAdj || words.length > 2) {
                    entities.add(entity);
                }
            }
        }

        // Remove substring entities (if "John Smith" exists, remove "John")
        List<String> result = new ArrayList<>(entities);
        result.removeIf(e -> {
            for (String other : entities) {
                if (!e.equals(other) && other.toLowerCase().contains(e.toLowerCase()) && other.length() > e.length()) {
                    return true;
                }
            }
            return false;
        });

        return result.stream().distinct().collect(Collectors.toList());
    }
}

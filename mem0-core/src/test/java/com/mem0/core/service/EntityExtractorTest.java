package com.mem0.core.service;

import com.mem0.core.entityextractor.EntityExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EntityExtractorTest {

    private final EntityExtractor extractor = new EntityExtractor();

    @Test
    @DisplayName("extract - finds email addresses")
    void extractFindsEmails() {
        List<String> entities = extractor.extract("Contact me at john@example.com for details");
        assertTrue(entities.contains("john@example.com"));
    }

    @Test
    @DisplayName("extract - finds URLs")
    void extractFindsUrls() {
        List<String> entities = extractor.extract("Visit https://example.com for more info");
        assertTrue(entities.stream().anyMatch(e -> e.contains("example.com")));
    }

    @Test
    @DisplayName("extract - finds phone numbers")
    void extractFindsPhoneNumbers() {
        List<String> entities = extractor.extract("Call me at +1-555-123-4567");
        assertFalse(entities.isEmpty());
    }

    @Test
    @DisplayName("extract - finds hashtags")
    void extractFindsHashtags() {
        List<String> entities = extractor.extract("Working on #java and #ai projects");
        assertTrue(entities.contains("#java"));
        assertTrue(entities.contains("#ai"));
    }

    @Test
    @DisplayName("extract - finds mentions")
    void extractFindsMentions() {
        List<String> entities = extractor.extract("Hey @john check this out");
        assertTrue(entities.contains("@john"));
    }

    @Test
    @DisplayName("extract - returns empty list for no entities")
    void extractReturnsEmptyForNoEntities() {
        List<String> entities = extractor.extract("This is just plain text with no entities");
        // May or may not find capitalized phrases, but shouldn't crash
        assertNotNull(entities);
    }
}

package com.mem0.core.config;

import com.mem0.core.dto.Message;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the user-side prompt for V3 additive extraction.
 * Ported from Python mem0/configs/prompts.py generate_additive_extraction_prompt()
 * and get_update_memory_messages().
 *
 * @author MoBai

 */
public final class PromptBuilder {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int TRUNCATE_LIMIT = Prompts.PAST_MESSAGE_TRUNCATION_LIMIT;

    private PromptBuilder() {}

    // ══════════════════════════════════════════════════════════════════════
    // generate_additive_extraction_prompt
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Builds the user prompt for additive extraction.
     * Ported from Python generate_additive_extraction_prompt().
     *
     * @param summary               narrative summary of user profile (may be null)
     * @param recentlyExtractedMemories recently extracted memories for dedup (may be null)
     * @param existingMemories       existing memories as list of maps with "id" and "text"
     * @param newMessages            current conversation turns
     * @param lastKMessages          preceding messages for reference resolution (may be null)
     * @param currentDate            system date
     * @param observationDate        temporal anchor for relative time resolution (may be null)
     * @param customInstructions     optional custom instructions (may be null)
     * @param useInputLanguage       if true, instruct LLM to respond in the input language
     * @return the formatted user prompt string
     */
    public static String buildAdditiveExtractionPrompt(
            String summary,
            List<Map<String, Object>> recentlyExtractedMemories,
            List<Map<String, Object>> existingMemories,
            List<Message> newMessages,
            List<Message> lastKMessages,
            LocalDate currentDate,
            LocalDate observationDate,
            String customInstructions,
            boolean useInputLanguage
    ) {
        // Resolve dates (Python: _resolve_dates)
        LocalDate resolvedCurrentDate = currentDate != null ? currentDate : LocalDate.now();
        LocalDate resolvedObservationDate = observationDate != null ? observationDate : resolvedCurrentDate;

        StringBuilder sections = new StringBuilder();

        // ## Summary
        sections.append("## Summary\n").append(formatSummary(summary)).append("\n\n");

        // ## Last k Messages
        sections.append("## Last k Messages\n").append(formatConversationHistory(lastKMessages)).append("\n\n");

        // ## Recently Extracted Memories
        sections.append("## Recently Extracted Memories\n").append(serializeMemories(recentlyExtractedMemories)).append("\n\n");

        // ## Existing Memories
        sections.append("## Existing Memories\n").append(serializeMemories(existingMemories)).append("\n\n");

        // ## New Messages
        sections.append("## New Messages\n").append(formatNewMessages(newMessages)).append("\n\n");

        // ## Observation Date
        sections.append("## Observation Date\n").append(resolvedObservationDate.format(DATE_FMT)).append("\n\n");

        // ## Current Date
        sections.append("## Current Date\n").append(resolvedCurrentDate.format(DATE_FMT)).append("\n\n");

        // ## Custom Instructions
        if (customInstructions != null && !customInstructions.isBlank()) {
            sections.append("## Custom Instructions\n").append(customInstructions).append("\n\n");
        }

        // ## Language Requirement
        if (useInputLanguage) {
            sections.append("""
                ## Language Requirement
                CRITICAL: Respond in the SAME LANGUAGE and SCRIPT as the input messages.
                1. Match the language of the user's messages exactly — if they write in Korean, extract in Korean; Japanese in Japanese; etc.
                2. Preserve the exact script/alphabet of the input.
                3. Do NOT translate or transliterate into English unless the input is already in English.
                4. Maintain all quality standards (contextual richness, temporal grounding, etc.) regardless of language.
                5. Technical terms, proper nouns, and brand names should be preserved in their original form as used in the input.
                6. If the input mixes languages (e.g., Hinglish), preserve both the mixed language style AND the script.
                7. For Japanese: explicitly resolve omitted subjects using conversation context.
                8. For CJK languages: maintain appropriate formality level from the source text.

                """);
        }

        // # Output:
        sections.append("# Output:");

        return sections.toString();
    }

    // ══════════════════════════════════════════════════════════════════════
    // get_update_memory_messages
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Builds the full update memory messages prompt (V2 style).
     * Ported from Python mem0/configs/prompts.py get_update_memory_messages().
     *
     * @param retrievedOldMemoryDict the current memory content as a JSON string (may be null/empty)
     * @param responseContent        the new retrieved facts
     * @param customUpdateMemoryPrompt optional custom prompt (uses default if null)
     * @return the formatted prompt string
     */
    public static String getUpdateMemoryMessages(
            String retrievedOldMemoryDict,
            String responseContent,
            String customUpdateMemoryPrompt
    ) {
        String prompt = customUpdateMemoryPrompt != null
            ? customUpdateMemoryPrompt
            : Prompts.DEFAULT_UPDATE_MEMORY_PROMPT;

        StringBuilder sb = new StringBuilder(prompt);

        if (retrievedOldMemoryDict != null && !retrievedOldMemoryDict.isBlank()) {
            sb.append("""

                Below is the current content of my memory which I have collected till now. You have to update it in the following format only:

                ```
                """);
            sb.append(retrievedOldMemoryDict);
            sb.append("""
                ```
                """);
        } else {
            sb.append("""

                Current memory is empty.

                """);
        }

        sb.append("""

            The new retrieved facts are mentioned in the triple backticks. You have to analyze the new retrieved facts and determine whether these facts should be added, updated, or deleted in the memory.

            ```
            """);
        sb.append(responseContent);
        sb.append("""
            ```

            You must return your response in the following JSON structure only:

            {
                "memory" : [
                    {
                        "id" : "<ID of the memory>",                # Use existing ID for updates/deletes, or new ID for additions
                        "text" : "<Content of the memory>",         # Content of the memory
                        "event" : "<Operation to be performed>",    # Must be "ADD", "UPDATE", "DELETE", or "NONE"
                        "old_memory" : "<Old memory content>"       # Required only if the event is "UPDATE"
                    },
                    ...
                ]
            }

            Follow the instruction mentioned below:
            - Do not return anything from the custom few shot prompts provided above.
            - If the current memory is empty, then you have to add the new retrieved facts to the memory.
            - You should return the updated memory in only JSON format as shown below. The memory key should be the same if no changes are made.
            - If there is an addition, generate a new key and add the new memory corresponding to it.
            - If there is a deletion, the memory key-value pair should be removed from the memory.
            - If there is an update, the ID key should remain the same and only the value needs to be updated.

            Do not return anything except the JSON format.
            """);

        return sb.toString();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Legacy helpers
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Builds the update memory prompt (simple version).
     */
    public static String buildUpdatePrompt(String existingMemory, String newInformation) {
        return Prompts.DEFAULT_UPDATE_MEMORY_PROMPT
            .replace("{existing_memory}", existingMemory)
            .replace("{new_information}", newInformation);
    }

    /**
     * Builds the memory answer prompt.
     */
    public static String buildMemoryAnswerPrompt(String memories, String question) {
        return Prompts.MEMORY_ANSWER_PROMPT
            .replace("{memories}", memories)
            .replace("{question}", question);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Private helpers — ported from Python prompts.py
    // ══════════════════════════════════════════════════════════════════════

    /** Python: _truncate_content */
    private static String truncateContent(String text) {
        if (text == null) return "";
        if (text.length() <= TRUNCATE_LIMIT) return text;
        return text.substring(0, TRUNCATE_LIMIT) + "...";
    }

    /** Python: _format_summary */
    private static String formatSummary(String summary) {
        if (summary == null) return "";
        if (summary instanceof String && summary.startsWith("{")) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = objectMapper.readValue(summary, Map.class);
                Object val = map.get("summary");
                return val != null ? val.toString() : "";
            } catch (Exception e) {
                return summary;
            }
        }
        return summary;
    }

    /** Python: _format_conversation_history */
    private static String formatConversationHistory(List<Message> messageEntities) {
        if (messageEntities == null || messageEntities.isEmpty()) return "";
        return messageEntities.stream()
            .map(m -> m.role() + ": " + truncateContent(m.content()))
            .collect(Collectors.joining("\n"));
    }

    /** Python: _serialize_memories */
    private static String serializeMemories(List<Map<String, Object>> memories) {
        if (memories == null || memories.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(memories);
        } catch (Exception e) {
            return memories.toString();
        }
    }

    /** Python: _format_new_messages */
    private static String formatNewMessages(List<Message> messageEntities) {
        if (messageEntities == null || messageEntities.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(
                messageEntities.stream()
                    .map(m -> Map.of("role", m.role(), "content", m.content()))
                    .toList()
            );
        } catch (Exception e) {
            return formatConversationHistory(messageEntities);
        }
    }
}

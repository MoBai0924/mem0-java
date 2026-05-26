package cn.hsine.mem0.core.reranker.impl;

import cn.hsine.mem0.core.llm.LLMProvider;
import cn.hsine.mem0.core.llm.LLMConfig;
import cn.hsine.mem0.core.dto.Message;
import cn.hsine.mem0.core.reranker.Reranker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM-based reranker implementation.
 * Uses any LLM provider to score document relevance.
 * Ported from Python mem0/reranker/llm_reranker.py.
 *
 * @author MoBai

 */
public class LLMReranker implements Reranker {

    private static final Logger log = LoggerFactory.getLogger(LLMReranker.class);
    private static final int MAX_INPUT_LEN = 4000;
    private static final Pattern SCORE_PATTERN = Pattern.compile("\\b([01](?:\\.\\d+)?)\\b");

    private final LLMProvider llmProvider;
    private final LLMConfig llmConfig;
    private final String scoringPrompt;

    private static final String DEFAULT_SCORING_SYSTEM_PROMPT = """
        You are a relevance scoring assistant. Score the relevance of the \
        document to the query on a scale of 0.0 to 1.0.
        Return ONLY a single number between 0.0 and 1.0.
        """;

    public LLMReranker(LLMProvider llmProvider) {
        this(llmProvider, new LLMConfig(), null);
    }

    public LLMReranker(LLMProvider llmProvider, LLMConfig llmConfig, String scoringPrompt) {
        this.llmProvider = llmProvider;
        this.llmConfig = llmConfig != null ? llmConfig : new LLMConfig();
        this.scoringPrompt = scoringPrompt != null ? scoringPrompt : DEFAULT_SCORING_SYSTEM_PROMPT;
    }

    @Override
    public List<Map<String, Object>> rerank(String query, List<Map<String, Object>> documents, int topK) {
        log.debug("Reranking {} documents for query: {}", documents.size(), query);

        List<Map<String, Object>> scored = new ArrayList<>();

        for (Map<String, Object> doc : documents) {
            String docText = extractText(doc);
            if (docText.length() > MAX_INPUT_LEN) {
                docText = docText.substring(0, MAX_INPUT_LEN);
            }

            double score = scoreDocument(query, docText);
            Map<String, Object> result = new LinkedHashMap<>(doc);
            result.put("rerank_score", score);
            scored.add(result);
        }

        scored.sort((a, b) -> Double.compare(
            (Double) b.getOrDefault("rerank_score", 0.0),
            (Double) a.getOrDefault("rerank_score", 0.0)
        ));

        return scored.size() <= topK ? scored : scored.subList(0, topK);
    }

    @Override
    public String getName() {
        return "llm_reranker";
    }

    private double scoreDocument(String query, String docText) {
        try {
            String userPrompt = "Query: " + query + "\n\nDocument: " + docText;
            List<Message> messages = List.of(
                Message.system(scoringPrompt),
                Message.user(userPrompt)
            );
            String response = llmProvider.generateResponse(messages, llmConfig);
            return extractScore(response);
        } catch (Exception e) {
            log.warn("Failed to score document, using default 0.5: {}", e.getMessage());
            return 0.5;
        }
    }

    private double extractScore(String response) {
        if (response == null || response.isBlank()) return 0.5;
        Matcher matcher = SCORE_PATTERN.matcher(response.trim());
        if (matcher.find()) {
            try {
                double score = Double.parseDouble(matcher.group(1));
                return Math.max(0.0, Math.min(1.0, score));
            } catch (NumberFormatException e) {
                return 0.5;
            }
        }
        return 0.5;
    }

    private String extractText(Map<String, Object> doc) {
        Object text = doc.get("memory");
        if (text == null) text = doc.get("text");
        if (text == null) text = doc.get("content");
        return text != null ? text.toString() : "";
    }
}

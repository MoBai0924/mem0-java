package cn.hsine.mem0.core.reranker.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * HuggingFace-compatible tokenizer for ONNX-based rerankers.
 * Loads a tokenizer.json file (HuggingFace tokenizers format) and
 * performs BPE tokenization with CLS/SEP special tokens.
 *
 * <p>Supports the standard HuggingFace tokenizers JSON format used by
 * models like cross-encoder/ms-marco-MiniLM-L-6-v2.
 *
 * @author MoBai

 */
public class HuggingFaceTokenizer implements Tokenizer {

    private static final String CLS_TOKEN = "[CLS]";
    private static final String SEP_TOKEN = "[SEP]";
    private static final String PAD_TOKEN = "[PAD]";
    private static final String UNK_TOKEN = "[UNK]";

    private final Map<String, Long> vocab;
    private final long clsId;
    private final long sepId;
    private final long padId;
    private final long unkId;
    private final int maxLength;

    /**
     * Creates a tokenizer from a model directory containing tokenizer.json.
     *
     * @param modelPath path to model directory or tokenizer.json file
     */
    public HuggingFaceTokenizer(String modelPath) {
        try {
            String tokenizerPath = modelPath.endsWith("tokenizer.json") ? modelPath : modelPath + "/tokenizer.json";
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new File(tokenizerPath));

            JsonNode modelNode = root.get("model");
            JsonNode vocabNode = modelNode.get("vocab");
            this.vocab = new HashMap<>();
            for (Map.Entry<String, JsonNode> entry : vocabNode.properties()) {
                vocab.put(entry.getKey(), entry.getValue().asLong());
            }
            this.clsId = vocab.getOrDefault(CLS_TOKEN, 101L);
            this.sepId = vocab.getOrDefault(SEP_TOKEN, 102L);
            this.padId = vocab.getOrDefault(PAD_TOKEN, 0L);
            this.unkId = vocab.getOrDefault(UNK_TOKEN, 1L);

            JsonNode truncationNode = root.get("truncation");
            if (truncationNode != null && !truncationNode.isNull()) {
                JsonNode maxLengthNode = truncationNode.path("max_length");
                this.maxLength = maxLengthNode.isNumber() ? maxLengthNode.asInt() : 512;
            } else {
                this.maxLength = 512;
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to load tokenizer from: " + modelPath, e);
        }
    }

    @Override
    public TokenizationResult tokenize(String query, String document) {
        List<Long> ids = new ArrayList<>();
        List<Long> mask = new ArrayList<>();
        List<Long> typeIds = new ArrayList<>();

        ids.add(clsId);
        mask.add(1L);
        typeIds.add(0L);

        List<Long> queryTokens = encode(query);
        for (Long tid : queryTokens) {
            ids.add(tid);
            mask.add(1L);
            typeIds.add(0L);
        }

        ids.add(sepId);
        mask.add(1L);
        typeIds.add(0L);

        List<Long> docTokens = encode(document);
        for (Long tid : docTokens) {
            ids.add(tid);
            mask.add(1L);
            typeIds.add(1L);
        }

        ids.add(sepId);
        mask.add(1L);
        typeIds.add(1L);

        if (ids.size() > maxLength) {
            ids = ids.subList(0, maxLength);
            mask = mask.subList(0, maxLength);
            typeIds = typeIds.subList(0, maxLength);
            ids.set(maxLength - 1, sepId);
        }

        return new TokenizationResult(
            ids.stream().mapToLong(Long::longValue).toArray(),
            mask.stream().mapToLong(Long::longValue).toArray(),
            typeIds.stream().mapToLong(Long::longValue).toArray()
        );
    }

    private List<Long> encode(String text) {
        List<Long> tokens = new ArrayList<>();
        String normalized = text.toLowerCase().trim();

        StringBuilder current = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (Character.isWhitespace(c)) {
                if (!current.isEmpty()) {
                    addToken(current.toString(), tokens);
                    current.setLength(0);
                }
                continue;
            }
            if (isPunctuation(c)) {
                if (current.length() > 0) {
                    addToken(current.toString(), tokens);
                    current.setLength(0);
                }
                addToken(String.valueOf(c), tokens);
                continue;
            }
            current.append(c);
        }
        if (!current.isEmpty()) {
            addToken(current.toString(), tokens);
        }

        return tokens;
    }

    private void addToken(String token, List<Long> tokens) {
        Long id = vocab.get(token);
        if (id != null) {
            tokens.add(id);
            return;
        }

        id = vocab.get("##" + token);
        if (id != null) {
            tokens.add(id);
            return;
        }

        for (int i = 0; i < token.length(); i++) {
            String sub = i == 0 ? String.valueOf(token.charAt(i)) : "##" + token.charAt(i);
            Long subId = vocab.get(sub);
            tokens.add(subId != null ? subId : unkId);
        }
    }

    private boolean isPunctuation(char c) {
        return "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~".indexOf(c) >= 0;
    }
}

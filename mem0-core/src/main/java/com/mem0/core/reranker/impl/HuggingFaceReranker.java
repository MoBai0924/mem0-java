package com.mem0.core.reranker.impl;

import ai.onnxruntime.*;
import com.mem0.core.reranker.Reranker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.LongBuffer;
import java.nio.FloatBuffer;
import java.util.*;

/**
 * HuggingFace Transformers reranker implementation using ONNX Runtime.
 * Loads a sequence-classification ONNX model for local reranking.
 * Ported from Python mem0/reranker/huggingface_reranker.py.
 *
 * <p>Requires an ONNX-serialized sequence classification model (e.g.,
 * BAAI/bge-reranker-base exported to ONNX format).
 *
 * <p>Features batch processing and optional min-max score normalization.
 *
 * @author MoBai

 */
public class HuggingFaceReranker implements Reranker {

    private static final Logger log = LoggerFactory.getLogger(HuggingFaceReranker.class);

    private final OrtEnvironment env;
    private final OrtSession session;
    private final Tokenizer tokenizer;
    private final int batchSize;
    private final int maxLength;
    private final boolean normalize;

    /**
     * Creates a HuggingFaceReranker with default settings.
     *
     * @param modelPath path to the ONNX model directory
     */
    public HuggingFaceReranker(String modelPath) {
        this(modelPath, 32, 512, true);
    }

    /**
     * Creates a HuggingFaceReranker.
     *
     * @param modelPath  path to the ONNX model directory (containing model.onnx and tokenizer.json)
     * @param batchSize  batch size for inference
     * @param maxLength  maximum sequence length for truncation
     * @param normalize  whether to normalize scores to [0, 1] range via min-max
     */
    public HuggingFaceReranker(String modelPath, int batchSize, int maxLength, boolean normalize) {
        this.batchSize = batchSize;
        this.maxLength = maxLength;
        this.normalize = normalize;
        try {
            this.env = OrtEnvironment.getEnvironment();
            String onnxPath = modelPath.endsWith(".onnx") ? modelPath : modelPath + "/model.onnx";
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            this.session = env.createSession(onnxPath, opts);
            this.tokenizer = new HuggingFaceTokenizer(modelPath);
            log.info("Loaded HuggingFace reranker model from: {}", modelPath);
        } catch (OrtException e) {
            throw new RuntimeException("Failed to load ONNX model from: " + modelPath, e);
        }
    }

    @Override
    public List<Map<String, Object>> rerank(String query, List<Map<String, Object>> documents, int topK) {
        log.debug("HuggingFace reranking {} documents", documents.size());

        if (documents.isEmpty()) {
            return documents;
        }

        try {
            List<String> docTexts = documents.stream()
                .map(this::extractText)
                .toList();

            List<Double> scores = new ArrayList<>();

            for (int i = 0; i < docTexts.size(); i += batchSize) {
                List<String> batch = docTexts.subList(i, Math.min(i + batchSize, docTexts.size()));
                List<double[]> batchScores = predictBatch(query, batch);
                for (double[] s : batchScores) {
                    scores.add(s[0]);
                }
            }

            if (normalize && scores.size() > 1) {
                double min = Collections.min(scores);
                double max = Collections.max(scores);
                double range = max - min + 1e-8;
                for (int i = 0; i < scores.size(); i++) {
                    scores.set(i, (scores.get(i) - min) / range);
                }
            }

            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < scores.size(); i++) {
                indices.add(i);
            }
            indices.sort((a, b) -> Double.compare(scores.get(b), scores.get(a)));

            int limit = Math.min(topK, indices.size());
            List<Map<String, Object>> reranked = new ArrayList<>();
            for (int i = 0; i < limit; i++) {
                int idx = indices.get(i);
                Map<String, Object> doc = new LinkedHashMap<>(documents.get(idx));
                doc.put("rerank_score", scores.get(idx));
                reranked.add(doc);
            }

            return reranked;

        } catch (Exception e) {
            log.error("HuggingFace rerank failed: {}", e.getMessage());
            for (Map<String, Object> doc : documents) {
                doc.put("rerank_score", 0.0);
            }
            return documents.size() <= topK ? documents : documents.subList(0, topK);
        }
    }

    @Override
    public String getName() {
        return "huggingface";
    }

    private List<double[]> predictBatch(String query, List<String> documents) throws OrtException {
        List<Tokenizer.TokenizationResult> tokenized = new ArrayList<>();
        int maxLen = 0;
        for (String doc : documents) {
            Tokenizer.TokenizationResult result = tokenizer.tokenize(query, doc);
            tokenized.add(result);
            maxLen = Math.max(maxLen, result.inputIds().length);
        }

        if (maxLen > maxLength) {
            maxLen = maxLength;
        }

        int batchSize = documents.size();
        long[] inputIds = new long[batchSize * maxLen];
        long[] attentionMask = new long[batchSize * maxLen];
        long[] tokenTypeIds = new long[batchSize * maxLen];

        for (int i = 0; i < batchSize; i++) {
            Tokenizer.TokenizationResult r = tokenized.get(i);
            int len = Math.min(r.inputIds().length, maxLen);
            System.arraycopy(r.inputIds(), 0, inputIds, i * maxLen, len);
            System.arraycopy(r.attentionMask(), 0, attentionMask, i * maxLen, len);
            if (r.tokenTypeIds() != null) {
                System.arraycopy(r.tokenTypeIds(), 0, tokenTypeIds, i * maxLen, len);
            }
        }

        long[] shape = {batchSize, maxLen};
        Map<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put("input_ids", OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds), shape));
        inputs.put("attention_mask", OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask), shape));
        inputs.put("token_type_ids", OnnxTensor.createTensor(env, LongBuffer.wrap(tokenTypeIds), shape));

        try (OrtSession.Result results = session.run(inputs)) {
            OnnxTensor logits = (OnnxTensor) results.get(0);
            Double[][] output = (Double[][]) logits.getValue();
            List<double[]> scores = new ArrayList<>();
            for (Double[] row : output) {
                double[] dRow = new double[row.length];
                for (int j = 0; j < row.length; j++) {
                    dRow[j] = row[j];
                }
                scores.add(dRow);
            }
            return scores;
        } finally {
            for (OnnxTensor t : inputs.values()) {
                t.close();
            }
        }
    }

    private String extractText(Map<String, Object> doc) {
        Object text = doc.get("memory");
        if (text == null) text = doc.get("text");
        if (text == null) text = doc.get("content");
        return text != null ? text.toString() : "";
    }
}

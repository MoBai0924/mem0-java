package cn.hsine.mem0.core.reranker.impl;

import ai.onnxruntime.*;
import cn.hsine.mem0.core.reranker.Reranker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.LongBuffer;
import java.util.*;

/**
 * Sentence Transformer reranker implementation using ONNX Runtime.
 * Loads a cross-encoder ONNX model for local reranking.
 * Ported from Python mem0/reranker/sentence_transformer_reranker.py.
 *
 * <p>Requires an ONNX-serialized cross-encoder model (e.g.,
 * cross-encoder/ms-marco-MiniLM-L-6-v2 exported to ONNX format).
 * Use the companion Python script to export:
 * <pre>
 * from sentence_transformers import CrossEncoder
 * model = CrossEncoder("cross-encoder/ms-marco-MiniLM-L-6-v2")
 * model.save("/path/to/model.onnx")
 * </pre>
 *
 * @author MoBai

 */
public class SentenceTransformerReranker implements Reranker {

    private static final Logger log = LoggerFactory.getLogger(SentenceTransformerReranker.class);

    private final OrtEnvironment env;
    private final OrtSession session;
    private final Tokenizer tokenizer;
    private final int batchSize;

    /**
     * Creates a SentenceTransformerReranker with default batch size (32).
     *
     * @param modelPath path to the ONNX model directory (containing model.onnx and tokenizer.json)
     */
    public SentenceTransformerReranker(String modelPath) {
        this(modelPath, 32);
    }

    /**
     * Creates a SentenceTransformerReranker.
     *
     * @param modelPath path to the ONNX model directory (containing model.onnx and tokenizer.json)
     * @param batchSize batch size for inference
     */
    public SentenceTransformerReranker(String modelPath, int batchSize) {
        this.batchSize = batchSize;
        try {
            this.env = OrtEnvironment.getEnvironment();
            String onnxPath = modelPath.endsWith(".onnx") ? modelPath : modelPath + "/model.onnx";
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            this.session = env.createSession(onnxPath, opts);
            this.tokenizer = new HuggingFaceTokenizer(modelPath);
            log.info("Loaded SentenceTransformer reranker model from: {}", modelPath);
        } catch (OrtException e) {
            throw new RuntimeException("Failed to load ONNX model from: " + modelPath, e);
        }
    }

    @Override
    public List<Map<String, Object>> rerank(String query, List<Map<String, Object>> documents, int topK) {
        log.debug("SentenceTransformer reranking {} documents", documents.size());

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

            List<int[]> indexed = new ArrayList<>();
            for (int i = 0; i < scores.size(); i++) {
                indexed.add(new int[]{i, i});
            }
            indexed.sort((a, b) -> Double.compare(scores.get(b[0]), scores.get(a[0])));

            int limit = Math.min(topK, indexed.size());
            List<Map<String, Object>> reranked = new ArrayList<>();
            for (int i = 0; i < limit; i++) {
                int idx = indexed.get(i)[0];
                Map<String, Object> doc = new LinkedHashMap<>(documents.get(idx));
                doc.put("rerank_score", scores.get(idx));
                reranked.add(doc);
            }

            return reranked;

        } catch (Exception e) {
            log.error("SentenceTransformer rerank failed: {}", e.getMessage());
            for (Map<String, Object> doc : documents) {
                doc.put("rerank_score", 0.0);
            }
            return documents.size() <= topK ? documents : documents.subList(0, topK);
        }
    }

    @Override
    public String getName() {
        return "sentence_transformer";
    }

    private List<double[]> predictBatch(String query, List<String> documents) throws OrtException {
        List<Tokenizer.TokenizationResult> tokenized = new ArrayList<>();
        int maxLen = 0;
        for (String doc : documents) {
            Tokenizer.TokenizationResult result = tokenizer.tokenize(query, doc);
            tokenized.add(result);
            maxLen = Math.max(maxLen, result.inputIds().length);
        }

        int batchSize = documents.size();
        long[] inputIds = new long[batchSize * maxLen];
        long[] attentionMask = new long[batchSize * maxLen];
        long[] tokenTypeIds = new long[batchSize * maxLen];

        for (int i = 0; i < batchSize; i++) {
            Tokenizer.TokenizationResult r = tokenized.get(i);
            System.arraycopy(r.inputIds(), 0, inputIds, i * maxLen, r.inputIds().length);
            System.arraycopy(r.attentionMask(), 0, attentionMask, i * maxLen, r.attentionMask().length);
            if (r.tokenTypeIds() != null) {
                System.arraycopy(r.tokenTypeIds(), 0, tokenTypeIds, i * maxLen, r.tokenTypeIds().length);
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

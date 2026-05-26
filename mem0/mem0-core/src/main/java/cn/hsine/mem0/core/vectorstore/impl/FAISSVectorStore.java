package cn.hsine.mem0.core.vectorstore.impl;

import cn.hsine.mem0.core.vectorstore.DistanceMetric;
import cn.hsine.mem0.core.vectorstore.SearchResult;
import cn.hsine.mem0.core.vectorstore.VectorEntry;
import cn.hsine.mem0.core.vectorstore.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FAISS vector store implementation for local file-based vector storage.
 * Uses an in-memory index with file persistence.
 *
 * @author MoBai

 */
public class FAISSVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(FAISSVectorStore.class);

    private final Map<String, VectorEntry> store = new ConcurrentHashMap<>();
    private final String indexPath;
    private int vectorSize = 1536;

    public FAISSVectorStore() {
        this("faiss_index");
    }

    public FAISSVectorStore(String indexPath) {
        this.indexPath = indexPath != null ? indexPath : "faiss_index";
        loadIndex();
        log.info("Initialized FAISS vector store at: {}", this.indexPath);
    }

    @Override
    public void createCollection(String name, int vectorSize, DistanceMetric metric) {
        this.vectorSize = vectorSize;
        log.info("FAISS collection created with vector size: {}", vectorSize);
    }

    @Override
    public boolean collectionExists(String name) {
        return true;
    }

    @Override
    public void deleteCollection(String name) {
        store.clear();
        saveIndex();
    }

    @Override
    public void insert(List<Double[]> vectors, List<Map<String, Object>> payloads, List<String> ids) {
        for (int i = 0; i < vectors.size(); i++) {
            store.put(ids.get(i), new VectorEntry(ids.get(i), vectors.get(i), payloads.get(i)));
        }
        saveIndex();
    }

    @Override
    public List<SearchResult> search(Double[] queryVector, int topK, Map<String, Object> filters) {
        List<SearchResult> candidates = new ArrayList<>();
        for (VectorEntry entry : store.values()) {
            if (filters != null && !matchesFilters(entry.payload(), filters)) {
                continue;
            }
            double score = cosineSimilarity(queryVector, entry.vector());

            String hash = "";
//            if (payload.containsKey("hash")) {
//                hash = String.valueOf(payload.get("hash"));
//            }

            candidates.add(new SearchResult(entry.id(), score, hash, entry.payload()));
        }
        candidates.sort((a, b) -> Double.compare(b.score(), a.score()));
        return candidates.subList(0, Math.min(topK, candidates.size()));
    }

    @Override
    public Optional<VectorEntry> get(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public void update(String id, Double[] vector, Map<String, Object> payload) {
        store.put(id, new VectorEntry(id, vector, payload));
        saveIndex();
    }

    @Override
    public void delete(String id) {
        store.remove(id);
        saveIndex();
    }

    @Override
    public List<VectorEntry> list(Map<String, Object> filters, Integer limit) {
        return store.values().stream()
                .filter(e -> filters == null || matchesFilters(e.payload(), filters))
                .limit(limit)
                .toList();
    }

    @Override
    public void reset() {
        store.clear();
        saveIndex();
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public String getName() {
        return "faiss";
    }

    private double cosineSimilarity(Double[] a, Double[] b) {
        if (a.length != b.length) {
            return 0.0;
        }
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private boolean matchesFilters(Map<String, Object> payload, Map<String, Object> filters) {
        if (payload == null) {
            return false;
        }
        for (var e : filters.entrySet()) {
            if (!e.getValue().equals(payload.get(e.getKey()))) {
                return false;
            }
        }
        return true;
    }

    private void saveIndex() {
        try {
            Path dir = Path.of(indexPath);
            Files.createDirectories(dir);
            Path file = dir.resolve("vectors.dat");
            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(file))) {
                oos.writeInt(store.size());
                for (var entry : store.entrySet()) {
                    oos.writeObject(entry.getKey());
                    oos.writeObject(entry.getValue().vector());
                    oos.writeObject(entry.getValue().payload());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to save FAISS index: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadIndex() {
        try {
            Path file = Path.of(indexPath, "vectors.dat");
            if (!Files.exists(file)) {
                return;
            }
            try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(file))) {
                int size = ois.readInt();
                for (int i = 0; i < size; i++) {
                    String id = (String) ois.readObject();
                    Double[] vector = (Double[]) ois.readObject();
                    Map<String, Object> payload = (Map<String, Object>) ois.readObject();
                    store.put(id, new VectorEntry(id, vector, payload));
                }
            }
            log.debug("Loaded {} vectors from FAISS index", store.size());
        } catch (Exception e) {
            log.warn("Failed to load FAISS index: {}", e.getMessage());
        }
    }
}

package com.mem0.core.domain.model;

import com.mem0.core.embedding.EmbeddingProvider;
import com.mem0.core.llm.LLMProvider;
import com.mem0.core.vectorstore.VectorStore;
import lombok.Data;

@Data
public class MemoryInstance {
    private String collectionName;
    private EmbeddingProvider embeddingModel;
    private VectorStore vectorStore;
    private LLMProvider llm;
    private String apiVersion;
}

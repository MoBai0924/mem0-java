package cn.hsine.mem0.core.config.vectorstore;

/**
 * Configuration for Supabase vector store provider.
 * Ported from Python mem0/configs/vectorstores/supabase.py.
 *
 * @author MoBai

 */
public class SupabaseConfig {

    private String connectionString;
    private String indexMethod = "hnsw";
    private String indexMeasure = "cosine";
    private String tableName = "memories";
    private int embeddingModelDims = 1536;

    public String getConnectionString() { return connectionString; }
    public void setConnectionString(String connectionString) { this.connectionString = connectionString; }
    public String getIndexMethod() { return indexMethod; }
    public void setIndexMethod(String indexMethod) { this.indexMethod = indexMethod; }
    public String getIndexMeasure() { return indexMeasure; }
    public void setIndexMeasure(String indexMeasure) { this.indexMeasure = indexMeasure; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public int getEmbeddingModelDims() { return embeddingModelDims; }
    public void setEmbeddingModelDims(int embeddingModelDims) { this.embeddingModelDims = embeddingModelDims; }
}

package cn.hsine.mem0.core.config.vectorstore;

/**
 * Configuration for S3 Vectors vector store provider.
 * Ported from Python mem0/configs/vectorstores/s3_vectors.py.
 *
 * @author MoBai

 */
public class S3VectorsConfig {

    private String vectorBucketName;
    private String distanceMetric = "COSINE";
    private String regionName;
    private int embeddingModelDims = 1536;

    public String getVectorBucketName() { return vectorBucketName; }
    public void setVectorBucketName(String vectorBucketName) { this.vectorBucketName = vectorBucketName; }
    public String getDistanceMetric() { return distanceMetric; }
    public void setDistanceMetric(String distanceMetric) { this.distanceMetric = distanceMetric; }
    public String getRegionName() { return regionName; }
    public void setRegionName(String regionName) { this.regionName = regionName; }
    public int getEmbeddingModelDims() { return embeddingModelDims; }
    public void setEmbeddingModelDims(int embeddingModelDims) { this.embeddingModelDims = embeddingModelDims; }
}

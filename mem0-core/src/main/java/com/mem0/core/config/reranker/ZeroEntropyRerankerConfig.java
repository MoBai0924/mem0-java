package com.mem0.core.config.reranker;

/**
 * Configuration for ZeroEntropy reranker provider.
 * Ported from Python mem0/configs/rerankers/zero_entropy.py.
 *
 * @author MoBai

 */
public class ZeroEntropyRerankerConfig extends BaseRerankerConfig {

    public ZeroEntropyRerankerConfig() {
        super();
        this.setModel("zerank-1");
    }
}

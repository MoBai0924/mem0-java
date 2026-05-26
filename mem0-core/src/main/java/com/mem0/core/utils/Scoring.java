package com.mem0.core.utils;

import com.mem0.core.score.Bm25Params;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mem0.core.utils.Lemmatizer.lemmatizeForBm25;

/**
 * Scoring utilities for hybrid search, ported from Python mem0/utils/scoring.py.
 *
 * @author MoBai

 */
public final class Scoring {

    /**
     * Entity boost weight constant.
     */
    public static final double ENTITY_BOOST_WEIGHT = 0.5;

    private Scoring() {
    }

    /**
     * 1:1 复刻 Python get_bm25_params
     * 根据查询词长度获取 BM25 归一化的 sigmoid 参数
     * 长查询的 BM25 原始分数更高，因此动态调整中点和斜率
     *
     * @param query      原始查询语句
     * @param lemmatized 分词后的查询语句（可为 null）
     * @return Bm25Params(midpoint 中点, steepness 斜率)
     */
    public static Bm25Params getBm25Params(String query, String lemmatized) {
        // 1. 如果 lemmatized 为 null，自动执行分词处理（对齐 Python 逻辑）
        String processedLemmatized = lemmatized;
        if (processedLemmatized == null) {
            processedLemmatized = lemmatizeForBm25(query);
        }

        // 2. 统计分词后的词条数量，空值则默认为 1（对齐 Python 逻辑）
        int numTerms;
        if (processedLemmatized.isBlank()) {
            numTerms = 1;
        } else {
            // 按空格分割，统计词条数
            numTerms = processedLemmatized.split("\\s+").length;
        }

        // 3. 严格按词条数返回对应参数（和 Python 数值完全一致）
        if (numTerms <= 3) {
            return new Bm25Params(5.0, 0.7);
        } else if (numTerms <= 6) {
            return new Bm25Params(7.0, 0.6);
        } else if (numTerms <= 9) {
            return new Bm25Params(9.0, 0.5);
        } else if (numTerms <= 15) {
            return new Bm25Params(10.0, 0.5);
        } else {
            return new Bm25Params(12.0, 0.5);
        }
    }

    /**
     * 1:1 复刻 Python normalize_bm25
     * 使用逻辑斯蒂 Sigmoid 函数将 BM25 原始分数归一化到 [0, 1] 区间
     *
     * @param rawScore  原始 BM25 分数（无界，通常 0~20+）
     * @param midpoint  Sigmoid 输出 0.5 对应的分数中点
     * @param steepness 控制 Sigmoid 曲线的陡峭程度
     * @return 归一化后的分数 [0, 1]
     */
    public static double normalizeBm25(double rawScore, double midpoint, double steepness) {
        // 严格复刻 Python 公式：1.0 / (1.0 + math.exp(-steepness * (raw_score - midpoint)))
        double exponent = -steepness * (rawScore - midpoint);
        return 1.0 / (1.0 + Math.exp(exponent));
    }

    /**
     * 1:1 复刻 Python score_and_rank 核心方法
     * 加权评分 + 排序 + 截断Top-K
     *
     * @param semanticResults 语义搜索候选集
     * @param bm25Scores      BM25关键词分数
     * @param entityBoosts    实体增强分数
     * @param threshold       语义分数最低阈值
     * @param topK            返回结果数量
     * @return 排序后的评分结果列表
     */
    public static List<Map<String, Object>> scoreAndRank(
            List<Map<String, Object>> semanticResults,
            Map<String, Double> bm25Scores,
            Map<String, Double> entityBoosts,
            Double threshold,
            Integer topK) {
        // 1. 判断是否存在BM25/实体增强信号
        boolean hasBm25 = !CollectionUtils.isEmpty(bm25Scores);
        boolean hasEntity = !CollectionUtils.isEmpty(entityBoosts);

        // 2. 计算动态分母（严格对齐Python规则）
        double maxPossible = 1.0;
        if (hasBm25) {
            maxPossible += 1.0;
        }
        if (hasEntity) {
            maxPossible += ENTITY_BOOST_WEIGHT;
        }

        List<Map<String, Object>> scoredList = new ArrayList<>();

        // 3. 遍历语义候选集，计算融合分数
        for (Map<String, Object> result : semanticResults) {
            // 过滤无ID的记录
            Object memIdObj = result.get("id");
            if (memIdObj == null) {
                continue;
            }

            // 过滤语义分低于阈值的记录（核心过滤规则）
            double semanticScore = (double) result.getOrDefault("score", 0.0);
            if (semanticScore < threshold) {
                continue;
            }

            // 获取各维度分数
            String memIdStr = memIdObj.toString();
            double bm25Score = bm25Scores.getOrDefault(memIdStr, 0.0);
            double entityBoost = entityBoosts.getOrDefault(memIdStr, 0.0);

            // 4. 计算融合分数（封顶1.0）
            double rawCombined = semanticScore + bm25Score + entityBoost;
            double combined = Math.min(rawCombined / maxPossible, 1.0);

            // 构建评分结果
            Map<String, Object> scoredItem = new HashMap<>();
            scoredItem.put("id", memIdStr);
            scoredItem.put("score", combined);
            scoredItem.put("payload", result.get("payload"));
            scoredList.add(scoredItem);
        }

        // 5. 按分数降序排序 + 截取Top-K
        return scoredList.stream()
                .sorted((a, b) -> Double.compare(
                        (double) b.get("score"),
                        (double) a.get("score")
                ))
                .limit(topK)
                .collect(Collectors.toList());
    }

}

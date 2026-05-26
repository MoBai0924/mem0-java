package com.mem0.core.dto.mid;

import java.util.Map;

// 实体返回：元数据 + 过滤器
public record FilterMetadataResult(
        Map<String, Object> processedMetadata,
        Map<String, Object> effectiveFilters
) {}

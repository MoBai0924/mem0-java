package com.mem0.core.utils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced metadata filter processing, ported from Python mem0/memory/main.py
 * _process_metadata_filters() and _has_advanced_operators().
 * Supports: eq, ne, gt, gte, lt, lte, in, nin, contains, icontains,
 *           AND, OR, NOT, wildcard (*)
 *
 * @author MoBai

 */
public final class MetadataFilter {

    private MetadataFilter() {}

    private static final Set<String> COMPARISON_OPS = Set.of(
        "eq", "ne", "gt", "gte", "lt", "lte", "in", "nin", "contains", "icontains"
    );

    private static final Set<String> LOGICAL_OPS = Set.of("AND", "OR", "NOT");

    /**
     * 1:1 复刻 Python _has_advanced_operators
     * 检查过滤器中是否包含需要特殊处理的高级运算符
     *
     * @param filters 过滤器字典
     * @return true=包含高级运算符，false=不包含
     */
    public static boolean hasAdvancedOperators(Map<String, Object> filters) {
        // 对应 Python: if not isinstance(filters, dict): return False
        if (filters == null) {
            return false;
        }

        // 遍历过滤器键值对
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // 1. 检查逻辑运算符 AND/OR/NOT
            if (LOGICAL_OPS.contains(key)) {
                return true;
            }

            // 2. 检查值为字典，包含比较运算符
            if (value instanceof Map<?, ?> valueMap) {
                for (Object opKey : valueMap.keySet()) {
                    if (COMPARISON_OPS.contains(opKey.toString())) {
                        return true;
                    }
                }
            }

            // 3. 检查通配符 *
            if ("*".equals(value)) {
                return true;
            }
        }

        // 无高级运算符
        return false;
    }

    /**
     * Processes metadata filters into a format suitable for vector store queries.
     * For simple filters (no advanced operators), returns the filters as-is.
     * For advanced filters, converts to vector-store-specific format.
     *
     * @param filters the input filters
     * @return processed filters
     */
    public static Map<String, Object> processMetadataFilters(Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) return filters;
        if (!hasAdvancedOperators(filters)) return filters;

        Map<String, Object> processed = new LinkedHashMap<>();
        for (var entry : filters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (LOGICAL_OPS.contains(key)) {
                // Handle logical operators
                processed.put("$" + key.toLowerCase(), processLogicalOperator(key, value));
            } else if (value instanceof Map) {
                // Handle comparison operators
                processed.put(key, processComparisonOperators(key, (Map<?, ?>) value));
            } else if ("*".equals(value)) {
                // Wildcard - any non-null value
                processed.put(key, Map.of("$exists", true));
            } else {
                // Simple equality
                processed.put(key, value);
            }
        }
        return processed;
    }

    private static Object processLogicalOperator(String op, Object value) {
        if (value instanceof List) {
            return ((List<?>) value).stream()
                .map(item -> {
                    if (item instanceof Map) return processMetadataFilters((Map<String, Object>) item);
                    return item;
                })
                .collect(Collectors.toList());
        }
        return value;
    }

    private static Map<String, Object> processComparisonOperators(String key, Map<?, ?> ops) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (var entry : ops.entrySet()) {
            String op = entry.getKey().toString();
            Object val = entry.getValue();

            result.put(switch (op) {
                case "eq" -> "$eq";
                case "ne" -> "$ne";
                case "gt" -> "$gt";
                case "gte" -> "$gte";
                case "lt" -> "$lt";
                case "lte" -> "$lte";
                case "in" -> "$in";
                case "nin" -> "$nin";
                case "contains" -> "$contains";
                case "icontains" -> "$icontains";
                default -> "$" + op;
            }, val);
        }
        return result;
    }

    /**
     * Evaluates a filter condition against a payload.
     * Used for in-memory filtering when the vector store doesn't support advanced operators.
     *
     * @param payload the document payload
     * @param filters the filters to evaluate
     * @return true if the payload matches all filters
     */
    public static boolean evaluate(Map<String, Object> payload, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) return true;
        if (payload == null) return false;

        for (var entry : filters.entrySet()) {
            String key = entry.getKey();
            Object filterValue = entry.getValue();

            if ("$and".equals(key)) {
                if (filterValue instanceof List) {
                    for (Object subFilter : (List<?>) filterValue) {
                        if (subFilter instanceof Map && !evaluate(payload, (Map<String, Object>) subFilter)) return false;
                    }
                }
            } else if ("$or".equals(key)) {
                if (filterValue instanceof List) {
                    boolean anyMatch = false;
                    for (Object subFilter : (List<?>) filterValue) {
                        if (subFilter instanceof Map && evaluate(payload, (Map<String, Object>) subFilter)) {
                            anyMatch = true;
                            break;
                        }
                    }
                    if (!anyMatch) return false;
                }
            } else if ("$not".equals(key)) {
                if (filterValue instanceof List && !((List<?>) filterValue).isEmpty()) {
                    Object subFilter = ((List<?>) filterValue).get(0);
                    if (subFilter instanceof Map && evaluate(payload, (Map<String, Object>) subFilter)) return false;
                }
            } else if (filterValue instanceof Map) {
                // Comparison operators
                Object payloadValue = payload.get(key);
                if (!evaluateComparison(payloadValue, (Map<?, ?>) filterValue)) return false;
            } else if ("*".equals(filterValue)) {
                // Wildcard - just check existence
                if (!payload.containsKey(key)) return false;
            } else {
                // Simple equality
                Object payloadValue = payload.get(key);
                if (!Objects.equals(String.valueOf(payloadValue), String.valueOf(filterValue))) return false;
            }
        }
        return true;
    }

    private static boolean evaluateComparison(Object payloadValue, Map<?, ?> ops) {
        for (var entry : ops.entrySet()) {
            String op = entry.getKey().toString();
            Object filterVal = entry.getValue();

            switch (op) {
                case "$eq" -> { if (!Objects.equals(payloadValue, filterVal)) return false; }
                case "$ne" -> { if (Objects.equals(payloadValue, filterVal)) return false; }
                case "$gt" -> { if (compareNumbers(payloadValue, filterVal) <= 0) return false; }
                case "$gte" -> { if (compareNumbers(payloadValue, filterVal) < 0) return false; }
                case "$lt" -> { if (compareNumbers(payloadValue, filterVal) >= 0) return false; }
                case "$lte" -> { if (compareNumbers(payloadValue, filterVal) > 0) return false; }
                case "$in" -> {
                    if (filterVal instanceof List && !((List<?>) filterVal).contains(payloadValue)) return false;
                }
                case "$nin" -> {
                    if (filterVal instanceof List && ((List<?>) filterVal).contains(payloadValue)) return false;
                }
                case "$contains" -> {
                    if (payloadValue == null || !String.valueOf(payloadValue).contains(String.valueOf(filterVal))) return false;
                }
                case "$icontains" -> {
                    if (payloadValue == null || !String.valueOf(payloadValue).toLowerCase().contains(String.valueOf(filterVal).toLowerCase())) return false;
                }
                case "$exists" -> {
                    boolean exists = (filterVal instanceof Boolean) ? (Boolean) filterVal : true;
                    if (exists && payloadValue == null) return false;
                    if (!exists && payloadValue != null) return false;
                }
            }
        }
        return true;
    }

    private static int compareNumbers(Object a, Object b) {
        try {
            return Double.compare(
                a instanceof Number ? ((Number) a).doubleValue() : Double.parseDouble(String.valueOf(a)),
                b instanceof Number ? ((Number) b).doubleValue() : Double.parseDouble(String.valueOf(b))
            );
        } catch (NumberFormatException e) {
            return String.valueOf(a).compareTo(String.valueOf(b));
        }
    }
}

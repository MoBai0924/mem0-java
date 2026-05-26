package cn.hsine.mem0.core.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

// 工具类：深拷贝（等价 Python deepcopy）
public class CopyUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public static Map<String, Object> deepCopy(Map<String, Object> source) {
        if (source == null) {
            return new HashMap<>();
        }
        return objectMapper.convertValue(source, Map.class);
    }
}

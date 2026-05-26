package com.mem0.core.message;

import java.util.Map;

public record MessageRecord(
        String id,
        String text,
        Double[] embedding,
        Map<String, Object> metadata
) {

}

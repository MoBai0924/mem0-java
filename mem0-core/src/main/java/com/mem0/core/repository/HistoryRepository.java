package com.mem0.core.repository;

import com.mem0.core.domain.model.MemoryHistory;

import java.util.List;
import java.util.Map;

public interface HistoryRepository {

    List<MemoryHistory> findByMemoryIdOrderByCreatedAtAsc(String memoryId);

    int insert(MemoryHistory history);

    int batchInsert(List<MemoryHistory> records);

    int deleteAll();

    List<Map<String, Object>> findByMemoryIdAsMap(String memoryId);
}

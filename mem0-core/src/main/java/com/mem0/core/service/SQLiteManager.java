package com.mem0.core.service;

import com.mem0.core.domain.model.MemoryHistory;
import com.mem0.core.dto.Message;
import com.mem0.core.dto.response.MemoryHistoryResponse;
import com.mem0.core.message.MessageEntity;
import com.mem0.core.repository.HistoryRepository;
import com.mem0.core.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 消息、历史存储
 * <p>
 * 由于python工程是使用sqlite，这里提取过来，统一由SQLiteManager对外交互。
 */
@Service
public class SQLiteManager {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private HistoryRepository historyRepository;


    public void addHistory(String memoryId, String oldMemory, String newMemory, String event,
                           Date createdAt, Date updatedAt, boolean isDeleted, String actorId, String role) {
        MemoryHistory h = new MemoryHistory();
        h.setId(UUID.randomUUID().toString());
        h.setMemoryId(memoryId);
        h.setOldMemory(oldMemory);
        h.setNewMemory(newMemory);
        h.setEvent(event);
        h.setCreatedAt(createdAt == null ? new Date() : createdAt);
        h.setUpdatedAt(updatedAt == null ? new Date() : updatedAt);
        h.setIsDeleted(isDeleted ? 1 : 0);
        h.setActorId(actorId);
        h.setRole(role);
        historyRepository.insert(h);
    }


    public void addHistory(String memoryId, String oldMemory, String newMemory, String event) {
        addHistory(memoryId, oldMemory, newMemory, event, null, null,
                false, null, null);
    }

    public void batchAddHistory(List<Map<String, Object>> records) {
        List<MemoryHistory> histories = records.stream().map(record -> {
            MemoryHistory h = new MemoryHistory();
            h.setId(UUID.randomUUID().toString());
            h.setMemoryId(toStr(record.get("memory_id")));
            h.setOldMemory(toStr(record.get("old_memory")));
            h.setNewMemory(toStr(record.get("new_memory")));
            h.setEvent(toStr(record.get("event")));
            h.setCreatedAt(new Date());
            h.setUpdatedAt(new Date());
            h.setIsDeleted(toInt(record.get("is_deleted"), 0));
            h.setActorId(toStr(record.get("actor_id")));
            h.setRole(toStr(record.get("role")));
            return h;
        }).toList();
        historyRepository.batchInsert(histories);
    }

    public List<Map<String, Object>> getHistory(String memoryId) {
        return historyRepository.findByMemoryIdAsMap(memoryId);
    }

    public void saveMessages(List<Message> messageEntities, String sessionScope) {
        if (messageEntities == null || messageEntities.isEmpty()) {
            return;
        }

        List<MessageEntity> msgList = messageEntities.stream().map(msg -> {
            MessageEntity m = new MessageEntity();
            m.setId(UUID.randomUUID().toString());
            m.setSessionScope(sessionScope);
            m.setRole(toStr(msg.role()));
            m.setContent(toStr(msg.content()));
            m.setName(toStr(msg.name()));
            m.setCreatedAt(new Date());
            return m;
        }).toList();
        messageRepository.batchInsert(msgList);
    }

    /**
     * 根据memoryId查询历史记录
     *
     * @param memoryId 记忆ID
     * @return 历史记忆记录
     */
    public List<MemoryHistoryResponse> history(String memoryId) {
        return historyRepository.findByMemoryIdOrderByCreatedAtAsc(memoryId).stream()
                .map(this::mapHistoryResponse).collect(Collectors.toList());
    }

    private MemoryHistoryResponse mapHistoryResponse(MemoryHistory history) {
        return new MemoryHistoryResponse(history.getId(), history.getMemoryId(), history.getOldMemory(),
                history.getNewMemory(), history.getEvent(), history.getActorId(),
                history.getRole(), history.getCreatedAt());
    }

    public List<MessageEntity> getLastMessages(String sessionScope, int limit) {
        return messageRepository.findByFilters(sessionScope, limit);
    }

    private static String toStr(Object value) {
        return value != null ? value.toString() : null;
    }

    private static int toInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}

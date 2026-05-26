package com.mem0.core.message;

import com.mem0.core.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository repository;
    private static final int DEFAULT_HISTORY_LIMIT = 10;

    public MessageService(MessageRepository repository) {
        this.repository = repository;
    }

    public MessageEntity saveMessage(String sessionScope, String role, String content, String name) {
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setId(UUID.randomUUID().toString());
        messageEntity.setSessionScope(sessionScope);
        messageEntity.setRole(role);
        messageEntity.setContent(content);
        messageEntity.setName(name);
        messageEntity.setCreatedAt(new Date());

        repository.insert(messageEntity);
        logger.debug("Saved messageEntity id={} for session={}", messageEntity.getId(), sessionScope);
        return messageEntity;
    }

    public void saveMessages(List<MessageEntity> messageEntities) {
        if (messageEntities == null || messageEntities.isEmpty()) {
            return;
        }
        repository.batchInsert(messageEntities);
        logger.debug("Batch saved {} messageEntities", messageEntities.size());
    }

    public MessageEntity getMessage(String id) {
        return repository.findById(id);
    }

    public List<MessageEntity> getMessagesBySession(String sessionScope) {
        return repository.findBySessionScope(sessionScope);
    }

    public List<MessageEntity> getLastMessages(String sessionScope, int limit) {
        int effectiveLimit = limit > 0 ? limit : DEFAULT_HISTORY_LIMIT;
        return repository.findByFilters(sessionScope, effectiveLimit);
    }

    public List<MessageEntity> getLastMessages(String sessionScope) {
        return getLastMessages(sessionScope, DEFAULT_HISTORY_LIMIT);
    }

    public List<MessageEntity> getMessagesBySessionAndRole(String sessionScope, String role) {
        return repository.findBySessionScopeAndRole(sessionScope, role);
    }

    public List<MessageEntity> getAllMessages() {
        return repository.findAll();
    }

    public boolean updateMessage(String id, String role, String content, String name) {
        MessageEntity existing = repository.findById(id);
        if (existing == null) {
            logger.warn("MessageEntity not found for update: id={}", id);
            return false;
        }

        if (role != null) {
            existing.setRole(role);
        }
        if (content != null) {
            existing.setContent(content);
        }
        if (name != null) {
            existing.setName(name);
        }

        repository.update(existing);
        logger.debug("Updated message id={}", id);
        return true;
    }

    public boolean deleteMessage(String id) {
        int deleted = repository.deleteById(id);
        if (deleted > 0) {
            logger.debug("Deleted message id={}", id);
            return true;
        }
        logger.warn("MessageEntity not found for delete: id={}", id);
        return false;
    }

    public int deleteMessagesBySession(String sessionScope) {
        int deleted = repository.deleteBySessionScope(sessionScope);
        logger.debug("Deleted {} messages for session={}", deleted, sessionScope);
        return deleted;
    }

    public int countMessagesBySession(String sessionScope) {
        return repository.countBySessionScope(sessionScope);
    }
}

package cn.hsine.mem0.core.repository;

import cn.hsine.mem0.core.message.MessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageRepository {

    MessageEntity findById(String id);

    List<MessageEntity> findBySessionScope(String sessionScope);

    List<MessageEntity> findByFilters(@Param("sessionScope") String sessionScope, @Param("limit") Integer limit);

    List<MessageEntity> findBySessionScopeAndRole(@Param("sessionScope") String sessionScope, @Param("role") String role);

    List<MessageEntity> findAll();

    int insert(MessageEntity messageEntity);

    int batchInsert(List<MessageEntity> messageEntities);

    int update(MessageEntity messageEntity);

    int deleteById(String id);

    int deleteBySessionScope(String sessionScope);

    int countBySessionScope(String sessionScope);
}

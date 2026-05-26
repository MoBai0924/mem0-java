package cn.hsine.mem0.core.repository;

import cn.hsine.mem0.core.domain.model.Memory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Memory entities using MyBatis.
 *
 * @author MoBai

 */
@Mapper
public interface MemoryRepository {

    Memory findById(String id);

    List<Memory> findAll();

    int save(Memory memory);

    int update(Memory memory);

    int delete(Memory memory);

    int deleteById(UUID id);

    void deleteAll();

    List<Memory> findByUserId(String userId);

    List<Memory> findByAgentId(String agentId);

    List<Memory> findByRunId(String runId);

    List<Memory> findByFilters(@Param("userId") String userId,
                               @Param("agentId") String agentId,
                               @Param("runId") String runId);

    void deleteByUserId(String userId);

    void deleteByAgentId(String agentId);

    void deleteByRunId(String runId);

    long countByUserId(String userId);

    long countByAgentId(String agentId);

    long countByRunId(String runId);

    Memory findByHash(String hash);

    long count();

    boolean existsById(UUID id);

    List<Memory> findByFiltersLimit(@Param("sessionScope") String sessionScope,@Param("") Integer limit);
}

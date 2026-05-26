package cn.hsine.mem0.server.repository;

import cn.hsine.mem0.server.domain.model.ApiKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository for ApiKey entities using MyBatis.
 *
 * @author MoBai

 */
@Mapper
public interface ApiKeyRepository {

    ApiKey findById(UUID id);

    ApiKey findByKeyHash(String keyHash);

    List<ApiKey> findByUserIdAndActive(@Param("userId") UUID userId, @Param("active") boolean active);

    int save(ApiKey apiKey);

    int update(ApiKey apiKey);

    long countByUserId(UUID userId);
}

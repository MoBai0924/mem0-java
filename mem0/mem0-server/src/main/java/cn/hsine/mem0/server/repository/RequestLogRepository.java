package cn.hsine.mem0.server.repository;

import cn.hsine.mem0.server.domain.model.RequestLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

/**
 * Repository for request log entries using MyBatis.
 *
 * @author MoBai

 */
@Mapper
public interface RequestLogRepository {

    int save(RequestLog requestLog);

    List<RequestLog> findByCreatedAtBetweenOrderByCreatedAtDesc(@Param("start") Instant start, @Param("end") Instant end);

    List<RequestLog> findByFilters(@Param("method") String method,
                                   @Param("statusCode") Integer statusCode,
                                   @Param("start") Instant start,
                                   @Param("end") Instant end);
}

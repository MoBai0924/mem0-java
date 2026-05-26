package com.mem0.server.repository;

import com.mem0.server.domain.model.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.UUID;

/**
 * Repository for User entities using MyBatis.
 *
 * @author MoBai

 */
@Mapper
public interface UserRepository {

    User findById(String id);

    User findByEmail(String email);

    int save(User user);

    int update(User user);

    boolean existsByEmail(String email);
}

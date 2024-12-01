package com.aleksandr_kobelskiy.week12practice.repository;

import com.aleksandr_kobelskiy.week12practice.entity.UserEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Mono;

public interface UserRepository extends R2dbcRepository<UserEntity, Long> {

    Mono<UserEntity> findByUsername(String username);

    @Modifying
    @Query("""
        UPDATE users 
        SET first_name = :firstName, last_name = :lastName, role = :role
        WHERE id = :id
    """)
    Mono<Long> updateUser(@Param("id") Long id,
                             @Param("firstName") String firstName,
                             @Param("lastName") String lastName,
                             @Param("role") String role);
}

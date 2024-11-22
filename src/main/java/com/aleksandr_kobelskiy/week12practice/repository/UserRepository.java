package com.aleksandr_kobelskiy.week12practice.repository;

import com.aleksandr_kobelskiy.week12practice.model.User;
//import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

//public interface UserRepository extends JpaRepository<User, Long> {
//    User findByUsername(String username);
//}

public interface UserRepository extends R2dbcRepository<User, Long> {
    Mono<User> findByUsername(String username);
}

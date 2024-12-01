package com.aleksandr_kobelskiy.week12practice.service;

import com.aleksandr_kobelskiy.week12practice.entity.Status;
import com.aleksandr_kobelskiy.week12practice.entity.UserEntity;
import com.aleksandr_kobelskiy.week12practice.entity.UserRole;
import com.aleksandr_kobelskiy.week12practice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Mono<UserEntity> registerUser(UserEntity user) {
        return userRepository.save(
                user.toBuilder()
                .password(passwordEncoder.encode(user.getPassword()))
                        .role(UserRole.USER)
                        .status(Status.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
        ).doOnSuccess(u -> {
            log.info("In registerUser - user: {} created", u);
        });
    }

    public Mono<UserEntity> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Mono<UserEntity> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Mono<UserEntity> getCurrentUser() {
        // Предполагается, что Spring Security используется для извлечения текущего пользователя
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName) // Получаем имя пользователя
                .flatMap(userRepository::findByUsername); // Находим пользователя в базе данных
    }

    public Mono<UserEntity> updateUser(Long id, String firstName, String lastName, String role) {
        return userRepository.findById(id)
                .flatMap(existingUser -> {
                    existingUser.setFirstName(firstName);
                    existingUser.setLastName(lastName);
                    existingUser.setRole(UserRole.valueOf(role));
                    return userRepository.updateUser(id, firstName, lastName, role)
                            .thenReturn(existingUser);
                });
    }

    public Flux<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    public Mono<Boolean> markUserAsDeleted(Long id) {
        return userRepository.findById(id)
                .flatMap(user -> {
                    if (user.getStatus().equals(Status.ACTIVE)) {
                        user.setStatus(Status.DELETED);
                        return userRepository.save(user).then(Mono.just(true));
                    }
                    return Mono.just(false); // Если статус уже DELETED или другой
                })
                .defaultIfEmpty(false);
    }
}

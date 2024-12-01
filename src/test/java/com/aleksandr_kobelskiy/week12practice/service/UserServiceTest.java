package com.aleksandr_kobelskiy.week12practice.service;

import com.aleksandr_kobelskiy.week12practice.entity.Status;
import com.aleksandr_kobelskiy.week12practice.entity.UserEntity;
import com.aleksandr_kobelskiy.week12practice.entity.UserRole;
import com.aleksandr_kobelskiy.week12practice.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Test registerUser successfully registers a user")
    public void givenValidUser_whenRegisterUser_thenUserIsRegistered() {
        UserEntity user = UserEntity.builder()
                .username("testUser")
                .password("password123")
                .build();

        UserEntity savedUser = user.toBuilder()
                .id(1L)
                .password("encodedPassword")
                .role(UserRole.USER)
                .status(Status.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(savedUser));

        Mono<UserEntity> result = userService.registerUser(user);

        StepVerifier.create(result)
                .expectNextMatches(u -> u.getId().equals(1L)
                        && u.getUsername().equals("testUser")
                        && u.getPassword().equals("encodedPassword")
                        && u.getRole() == UserRole.USER
                        && u.getStatus() == Status.ACTIVE)
                .verifyComplete();

        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepository, times(1)).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Test registerUser fails when userRepository.save() fails")
    public void givenUserRepositoryFails_whenRegisterUser_thenErrorIsReturned() {
        UserEntity user = UserEntity.builder()
                .username("testUser")
                .password("password123")
                .build();

        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.error(new RuntimeException("Database error")));

        Mono<UserEntity> result = userService.registerUser(user);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && throwable.getMessage().equals("Database error"))
                .verify();

        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepository, times(1)).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Test getUserById returns user when user exists")
    public void givenExistingUserId_whenGetUserById_thenUserIsReturned() {
        Long userId = 1L;
        UserEntity user = UserEntity.builder()
                .id(userId)
                .username("testUser")
                .build();

        when(userRepository.findById(userId)).thenReturn(Mono.just(user));

        Mono<UserEntity> result = userService.getUserById(userId);

        StepVerifier.create(result)
                .expectNext(user)
                .verifyComplete();

        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("Test getUserById returns empty when user does not exist")
    public void givenNonExistingUserId_whenGetUserById_thenEmptyIsReturned() {
        Long userId = 1L;

        when(userRepository.findById(userId)).thenReturn(Mono.empty());

        Mono<UserEntity> result = userService.getUserById(userId);

        StepVerifier.create(result)
                .verifyComplete();

        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("Test getUserByUsername returns user when user exists")
    public void givenExistingUsername_whenGetUserByUsername_thenUserIsReturned() {
        String username = "testUser";
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username(username)
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Mono.just(user));

        Mono<UserEntity> result = userService.getUserByUsername(username);

        StepVerifier.create(result)
                .expectNext(user)
                .verifyComplete();

        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    @DisplayName("Test getUserByUsername returns empty when user does not exist")
    public void givenNonExistingUsername_whenGetUserByUsername_thenEmptyIsReturned() {
        String username = "nonExistingUser";

        when(userRepository.findByUsername(username)).thenReturn(Mono.empty());

        Mono<UserEntity> result = userService.getUserByUsername(username);

        StepVerifier.create(result)
                .verifyComplete();

        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    @DisplayName("Test getCurrentUser returns current user when authenticated")
    public void whenUserIsAuthenticated_thenGetCurrentUserReturnsUser() {
        String username = "testUser";
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username(username)
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(username);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        when(userRepository.findByUsername(username)).thenReturn(Mono.just(user));

        Mono<UserEntity> result = userService.getCurrentUser()
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));

        StepVerifier.create(result)
                .expectNext(user)
                .verifyComplete();

        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    @DisplayName("Test getCurrentUser returns empty when not authenticated")
    public void whenUserIsNotAuthenticated_thenGetCurrentUserReturnsEmpty() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(false);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        Mono<UserEntity> result = userService.getCurrentUser()
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));

        StepVerifier.create(result)
                .verifyComplete();

        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("Test updateUser successfully updates user when user exists")
    public void givenExistingUser_whenUpdateUser_thenUserIsUpdated() {
        Long userId = 1L;
        String firstName = "John";
        String lastName = "Doe";
        String role = "ADMIN";

        UserEntity existingUser = UserEntity.builder()
                .id(userId)
                .username("testUser")
                .firstName("OldFirstName")
                .lastName("OldLastName")
                .role(UserRole.USER)
                .build();

        UserEntity updatedUser = existingUser.toBuilder()
                .firstName(firstName)
                .lastName(lastName)
                .role(UserRole.valueOf(role))
                .build();

        when(userRepository.findById(userId)).thenReturn(Mono.just(existingUser));
        when(userRepository.updateUser(userId, firstName, lastName, role)).thenReturn(Mono.empty());

        Mono<UserEntity> result = userService.updateUser(userId, firstName, lastName, role);

        StepVerifier.create(result)
                .expectNext(updatedUser)
                .verifyComplete();

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).updateUser(userId, firstName, lastName, role);
    }

    @Test
    @DisplayName("Test updateUser returns empty when user does not exist")
    public void givenNonExistingUser_whenUpdateUser_thenEmptyIsReturned() {
        Long userId = 1L;
        String firstName = "John";
        String lastName = "Doe";
        String role = "ADMIN";

        when(userRepository.findById(userId)).thenReturn(Mono.empty());

        Mono<UserEntity> result = userService.updateUser(userId, firstName, lastName, role);

        StepVerifier.create(result)
                .verifyComplete();

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).updateUser(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Test getAllUsers returns all users")
    public void whenGetAllUsers_thenAllUsersAreReturned() {
        UserEntity user1 = UserEntity.builder()
                .id(1L)
                .username("user1")
                .build();
        UserEntity user2 = UserEntity.builder()
                .id(2L)
                .username("user2")
                .build();

        when(userRepository.findAll()).thenReturn(Flux.just(user1, user2));

        Flux<UserEntity> result = userService.getAllUsers();

        StepVerifier.create(result)
                .expectNext(user1, user2)
                .verifyComplete();

        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Test getAllUsers returns empty when no users")
    public void whenGetAllUsersAndNoUsersExist_thenEmptyFluxIsReturned() {
        when(userRepository.findAll()).thenReturn(Flux.empty());

        Flux<UserEntity> result = userService.getAllUsers();

        StepVerifier.create(result)
                .verifyComplete();

        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Test markUserAsDeleted marks user as deleted when user is ACTIVE")
    public void givenActiveUser_whenMarkUserAsDeleted_thenUserIsMarkedDeleted() {
        Long userId = 1L;
        UserEntity activeUser = UserEntity.builder()
                .id(userId)
                .status(Status.ACTIVE)
                .build();

        UserEntity deletedUser = activeUser.toBuilder()
                .status(Status.DELETED)
                .build();

        when(userRepository.findById(userId)).thenReturn(Mono.just(activeUser));
        when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(deletedUser));

        Mono<Boolean> result = userService.markUserAsDeleted(userId);

        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Test markUserAsDeleted returns false when user does not exist")
    public void givenNonExistingUser_whenMarkUserAsDeleted_thenReturnsFalse() {
        Long userId = 1L;

        when(userRepository.findById(userId)).thenReturn(Mono.empty());

        Mono<Boolean> result = userService.markUserAsDeleted(userId);

        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).save(any(UserEntity.class));
    }
}
package com.aleksandr_kobelskiy.week12practice.rest;

import com.aleksandr_kobelskiy.week12practice.config.TestMethodSecurityConfig;
import com.aleksandr_kobelskiy.week12practice.config.WebSecurityConfig;
import com.aleksandr_kobelskiy.week12practice.dto.UserUpdateRequestDto;
import com.aleksandr_kobelskiy.week12practice.entity.UserEntity;
import com.aleksandr_kobelskiy.week12practice.entity.UserRole;
import com.aleksandr_kobelskiy.week12practice.security.AuthenticationManager;
import com.aleksandr_kobelskiy.week12practice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = UserRestControllerV1.class)
@Import({WebSecurityConfig.class, TestMethodSecurityConfig.class})
class UserRestControllerV1Test {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @BeforeEach
    public void setup() {
        this.webTestClient = this.webTestClient.mutateWith(csrf());
    }

    @Test
    @DisplayName("Успешное обновление пользователя администратором")
    public void testUpdateUserAsAdminSuccess() {
        Long userId = 1L;
        UserUpdateRequestDto request = new UserUpdateRequestDto("John", "Doe", UserRole.USER);
        UserEntity updatedUser = UserEntity.builder()
                .id(userId)
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.USER)
                .build();

        Mockito.when(userService.updateUser(eq(userId), eq("John"), eq("Doe"), eq("USER")))
                .thenReturn(Mono.just(updatedUser));

        webTestClient.mutateWith(mockUser().roles("ADMIN"))
                .put()
                .uri("/api/v1/users/{id}", userId)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(userId)
                .jsonPath("$.firstName").isEqualTo("John")
                .jsonPath("$.lastName").isEqualTo("Doe")
                .jsonPath("$.role").isEqualTo("USER");
    }

    @Test
    @DisplayName("Обновление пользователя запрещено для не администратора")
    public void testUpdateUserAsNonAdminForbidden() {
        Long userId = 1L;
        UserUpdateRequestDto request = new UserUpdateRequestDto("John", "Doe", UserRole.USER);

        webTestClient.mutateWith(mockUser().roles("USER"))
                .put()
                .uri("/api/v1/users/{id}", userId)
                .bodyValue(request)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Успешное получение всех пользователей модератором")
    public void testGetAllUsersAsModeratorSuccess() {
        UserEntity user1 = UserEntity.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.USER)
                .build();

        UserEntity user2 = UserEntity.builder()
                .id(2L)
                .firstName("Jane")
                .lastName("Smith")
                .role(UserRole.USER)
                .build();

        Mockito.when(userService.getAllUsers()).thenReturn(Flux.just(user1, user2));

        webTestClient.mutateWith(mockUser().roles("MODERATOR"))
                .get()
                .uri("/api/v1/users")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(1)
                .jsonPath("$[1].id").isEqualTo(2);
    }

    @Test
    @DisplayName("Получение всех пользователей запрещено для неавторизованного пользователя")
    public void testGetAllUsersAsUserForbidden() {
        webTestClient.mutateWith(mockUser().roles("USER"))
                .get()
                .uri("/api/v1/users")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Успешное получение пользователя по ID администратором")
    public void testGetUserByIdAsAdminSuccess() {
        Long userId = 1L;
        UserEntity user = UserEntity.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.USER)
                .build();

        Mockito.when(userService.getUserById(eq(userId))).thenReturn(Mono.just(user));

        webTestClient.mutateWith(mockUser().roles("ADMIN"))
                .get()
                .uri("/api/v1/users/{id}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(userId)
                .jsonPath("$.firstName").isEqualTo("John")
                .jsonPath("$.lastName").isEqualTo("Doe")
                .jsonPath("$.role").isEqualTo("USER");
    }

    @Test
    @DisplayName("Получение пользователя по ID запрещено для не администратора")
    public void testGetUserByIdAsNonAdminForbidden() {
        Long userId = 1L;

        webTestClient.mutateWith(mockUser().roles("USER"))
                .get()
                .uri("/api/v1/users/{id}", userId)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Успешное удаление пользователя администратором")
    public void testDeleteUserByIdAsAdminSuccess() {
        Long userId = 1L;

        Mockito.when(userService.markUserAsDeleted(eq(userId))).thenReturn(Mono.just(true));

        webTestClient.mutateWith(mockUser().roles("ADMIN"))
                .delete()
                .uri("/api/v1/users/{id}", userId)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("Удаление пользователя запрещено для не администратора")
    public void testDeleteUserByIdAsNonAdminForbidden() {
        Long userId = 1L;

        webTestClient.mutateWith(mockUser().roles("USER"))
                .delete()
                .uri("/api/v1/users/{id}", userId)
                .exchange()
                .expectStatus().isForbidden();
    }
}
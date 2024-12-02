package com.aleksandr_kobelskiy.week12practice.it;

import com.aleksandr_kobelskiy.week12practice.config.MySqlTestcontainerConfig;
import com.aleksandr_kobelskiy.week12practice.dto.UserUpdateRequestDto;
import com.aleksandr_kobelskiy.week12practice.entity.Status;
import com.aleksandr_kobelskiy.week12practice.entity.UserEntity;
import com.aleksandr_kobelskiy.week12practice.entity.UserRole;
import com.aleksandr_kobelskiy.week12practice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MySQLContainer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(MySqlTestcontainerConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
//@ActiveProfiles("test")
public class itUserRestControllerV1Test {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebTestClient webTestClient;

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        MySQLContainer<?> mysqlContainer = MySqlTestcontainerConfig.mysqlContainer;
        registry.add("spring.r2dbc.url", () -> String.format("r2dbc:mysql://%s:%d/%s",
                mysqlContainer.getHost(),
                mysqlContainer.getFirstMappedPort(),
                mysqlContainer.getDatabaseName()));
        registry.add("spring.r2dbc.username", mysqlContainer::getUsername);
        registry.add("spring.r2dbc.password", mysqlContainer::getPassword);

        // Настраиваем Flyway
        registry.add("spring.flyway.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.flyway.user", mysqlContainer::getUsername);
        registry.add("spring.flyway.password", mysqlContainer::getPassword);
    }

    @BeforeEach
    public void setUp() {
        userRepository.deleteAll().block();
    }

    @Test
    @DisplayName("Успешное обновление пользователя администратором")
    public void testUpdateUserAsAdminSuccess() {

        // Создаем пользователя в базе данных
        UserEntity user = UserEntity.builder()
                .username("testuser")
                .password("password123") // если требуется
                .firstName("InitialFirstName")
                .lastName("InitialLastName")
                .role(UserRole.USER)
                .build();
        user = userRepository.save(user).block();
        assert user != null;
        Long userId = user.getId();

        // Готовим данные для обновления
        UserUpdateRequestDto request = new UserUpdateRequestDto("John", "Doe", UserRole.USER);

        // Выполняем запрос на обновление
        webTestClient.mutateWith(mockUser().roles("ADMIN"))
                .put()
                .uri("/api/v1/users/{id}", userId)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(userId.intValue())
                .jsonPath("$.firstName").isEqualTo("John")
                .jsonPath("$.lastName").isEqualTo("Doe")
                .jsonPath("$.role").isEqualTo("USER");

        // Проверяем, что данные в базе обновились
        UserEntity updatedUser = userRepository.findById(userId).block();
        assertNotNull(updatedUser);
        assertEquals("John", updatedUser.getFirstName());
        assertEquals("Doe", updatedUser.getLastName());
        assertEquals(UserRole.USER, updatedUser.getRole());
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
        // Создаем пользователей
        final UserEntity user1 = UserEntity.builder()
                .username("user1")
                .password("password1")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.USER)
                .build();

        final UserEntity user2 = UserEntity.builder()
                .username("user2")
                .password("password2")
                .firstName("Jane")
                .lastName("Smith")
                .role(UserRole.USER)
                .build();

        // Сохраняем пользователей и получаем сохраненные экземпляры
        final UserEntity savedUser1 = userRepository.save(user1).block();
        final UserEntity savedUser2 = userRepository.save(user2).block();

        // Выполняем GET запрос к /api/v1/users
        webTestClient.mutateWith(mockUser().roles("MODERATOR"))
                .get()
                .uri("/api/v1/users")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(UserEntity.class)
                .hasSize(2)
                .consumeWith(response -> {
                    List<UserEntity> users = response.getResponseBody();
                    assertNotNull(users);
                    // Проверяем, что оба пользователя присутствуют в ответе
                    assertTrue(users.stream().anyMatch(u -> u.getId().equals(savedUser1.getId())
                            && u.getFirstName().equals("John") && u.getLastName().equals("Doe")));
                    assertTrue(users.stream().anyMatch(u -> u.getId().equals(savedUser2.getId())
                            && u.getFirstName().equals("Jane") && u.getLastName().equals("Smith")));
                });
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
        // Создаем пользователя
        UserEntity user = UserEntity.builder()
                .username("testuser")
                .password("password123") // если требуется
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.USER)
                .build();

        // Сохраняем пользователя в базе данных
        UserEntity savedUser = userRepository.save(user).block();
        Long userId = savedUser.getId();

        // Выполняем GET запрос к /api/v1/users/{id}
        webTestClient.mutateWith(mockUser().roles("ADMIN"))
                .get()
                .uri("/api/v1/users/{id}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(userId.intValue())
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
        // Создаем пользователя
        UserEntity user = UserEntity.builder()
                .username("testuser")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.USER)
                .status(Status.ACTIVE) // Предполагаем, что есть статус ACTIVE
                .build();

        // Сохраняем пользователя в базе данных
        UserEntity savedUser = userRepository.save(user).block();
        Long userId = savedUser.getId();

        // Выполняем DELETE запрос к /api/v1/users/{id}
        webTestClient.mutateWith(mockUser().roles("ADMIN"))
                .delete()
                .uri("/api/v1/users/{id}", userId)
                .exchange()
                .expectStatus().isNoContent();

        // Проверяем, что пользователь был помечен как удаленный
        UserEntity deletedUser = userRepository.findById(userId).block();
        assertNotNull(deletedUser);
        assertEquals(Status.DELETED, deletedUser.getStatus());
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

package com.aleksandr_kobelskiy.week12practice.it;

import com.aleksandr_kobelskiy.week12practice.config.MySqlTestcontainerConfig;
import com.aleksandr_kobelskiy.week12practice.dto.AuthRequestDto;
import com.aleksandr_kobelskiy.week12practice.dto.AuthResponseDto;
import com.aleksandr_kobelskiy.week12practice.dto.UserDto;
import com.aleksandr_kobelskiy.week12practice.entity.Status;
import com.aleksandr_kobelskiy.week12practice.entity.UserEntity;
import com.aleksandr_kobelskiy.week12practice.entity.UserRole;
import com.aleksandr_kobelskiy.week12practice.repository.UserRepository;
import com.aleksandr_kobelskiy.week12practice.security.SecurityService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MySQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(MySqlTestcontainerConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class itAuthRestControllerV1Test {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private SecurityService securityService;

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
        // Очищаем базу данных перед началом каждого теста
        userRepository.deleteAll().block();
    }

    @Test
    @DisplayName("Успешная регистрация пользователя")
    public void testRegisterUserSuccess() {
        UserDto userDto = new UserDto();
        userDto.setUsername("testuser");
        userDto.setPassword("password123");
        userDto.setFirstName("John");
        userDto.setLastName("Doe");
        userDto.setRole(UserRole.USER);

        webTestClient.post()
                .uri("/api/v1/auth/register")
                .bodyValue(userDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserDto.class)
                .value(response -> {
                    Assertions.assertNotNull(response.getId());
                    Assertions.assertEquals("testuser", response.getUsername());
                    Assertions.assertEquals("John", response.getFirstName());
                    Assertions.assertEquals("Doe", response.getLastName());
                    Assertions.assertEquals(UserRole.USER, response.getRole());
                });

        // Проверяем, что пользователь сохранен в базе данных
        UserEntity savedUser = userRepository.findByUsername("testuser").block();
        Assertions.assertNotNull(savedUser);
        Assertions.assertEquals("John", savedUser.getFirstName());
        Assertions.assertEquals("Doe", savedUser.getLastName());
        Assertions.assertEquals(UserRole.USER, savedUser.getRole());
    }

    @Test
    @DisplayName("Неуспешная регистрация пользователя с уже существующим именем пользователя")
    public void testRegisterUserFailureUsernameExists() {
        // Сначала создаем пользователя в базе данных
        UserEntity existingUser = UserEntity.builder()
                .username("testuser")
                .password(passwordEncoder.encode("password123"))
                .firstName("Existing")
                .lastName("User")
                .role(UserRole.USER)
                .build();
        userRepository.save(existingUser).block();

        // Пытаемся зарегистрировать пользователя с тем же именем пользователя
        UserDto userDto = new UserDto();
        userDto.setUsername("testuser");
        userDto.setPassword("newpassword");
        userDto.setFirstName("John");
        userDto.setLastName("Doe");
        userDto.setRole(UserRole.USER);

        webTestClient.post()
                .uri("/api/v1/auth/register")
                .bodyValue(userDto)
                .exchange()
                .expectStatus().isBadRequest(); // Проверяем только статус ответа
    }

    @Test
    @DisplayName("Успешная аутентификация пользователя")
    public void testLoginSuccess() {
        // Сначала регистрируем пользователя
        UserEntity user = UserEntity.builder()
                .username("testuser")
                .password(passwordEncoder.encode("password123"))
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.USER)
                .status(Status.ACTIVE)
                .build();

        // Сохраняем пользователя в базе данных
        userRepository.save(user).block();

        AuthRequestDto authRequest = new AuthRequestDto();
        authRequest.setUsername("testuser");
        authRequest.setPassword("password123");

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .bodyValue(authRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponseDto.class)
                .value(response -> {
                    Assertions.assertNotNull(response.getToken());
                    Assertions.assertNotNull(response.getUserId());
                    Assertions.assertNotNull(response.getIssuedAt());
                    Assertions.assertNotNull(response.getExpiresAt());
                });
    }

    @Test
    @DisplayName("Неуспешная аутентификация с неверным паролем")
    public void testLoginFailureInvalidPassword() {
        // Создаем и сохраняем пользователя
        UserEntity user = UserEntity.builder()
                .username("testuser")
                .password(passwordEncoder.encode("correctpassword"))
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.USER)
                .status(Status.ACTIVE)
                .build();
        userRepository.save(user).block();

        AuthRequestDto authRequest = new AuthRequestDto();
        authRequest.setUsername("testuser");
        authRequest.setPassword("wrongpassword");

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .bodyValue(authRequest)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.message").exists();
    }

    @Test
    @DisplayName("Успешное получение информации о текущем пользователе")
    public void testGetUserInfoSuccess() {
        // Шаг 1: Создаем и сохраняем пользователя
        UserEntity user = UserEntity.builder()
                .username("testuser")
                .password(passwordEncoder.encode("password123"))
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.USER)
                .status(Status.ACTIVE)
                .build();

        UserEntity savedUser = userRepository.save(user).block();

        // Шаг 2: Выполняем аутентификацию для получения токена
        AuthRequestDto authRequest = new AuthRequestDto();
        authRequest.setUsername("testuser");
        authRequest.setPassword("password123");

        AuthResponseDto authResponse = webTestClient.post()
                .uri("/api/v1/auth/login")
                .bodyValue(authRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponseDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(authResponse, "AuthResponse должен быть не null");
        Assertions.assertNotNull(authResponse.getToken(), "Токен должен быть не null");

        String token = authResponse.getToken();

        // Шаг 3: Выполняем защищенный запрос на получение информации о текущем пользователе
        webTestClient.get()
                .uri("/api/v1/auth/info")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserDto.class)
                .value(response -> {
                    Assertions.assertEquals(savedUser.getId(), response.getId(), "ID пользователя должен совпадать");
                    Assertions.assertEquals("testuser", response.getUsername(), "Имя пользователя должно совпадать");
                    Assertions.assertEquals("John", response.getFirstName(), "Имя должно совпадать");
                    Assertions.assertEquals("Doe", response.getLastName(), "Фамилия должна совпадать");
                    Assertions.assertEquals(UserRole.USER, response.getRole(), "Роль должна совпадать");
                });
    }

    @Test
    @DisplayName("Неуспешное получение информации о текущем пользователе без токена")
    public void testGetUserInfoFailureNoToken() {
        webTestClient.get()
                .uri("/api/v1/auth/info")
                .exchange()
                .expectStatus().isUnauthorized(); // Проверяем только статус ответа
    }
}

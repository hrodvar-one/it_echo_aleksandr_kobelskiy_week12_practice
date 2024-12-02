package com.aleksandr_kobelskiy.week12practice.it;

import com.aleksandr_kobelskiy.week12practice.config.MySqlTestcontainerConfig;
import com.aleksandr_kobelskiy.week12practice.entity.*;
import com.aleksandr_kobelskiy.week12practice.repository.EventRepository;
import com.aleksandr_kobelskiy.week12practice.repository.FileRepository;
import com.aleksandr_kobelskiy.week12practice.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MySQLContainer;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(MySqlTestcontainerConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class itEventRestControllerV1Test {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileRepository fileRepository;

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
        eventRepository.deleteAll()
                .then(fileRepository.deleteAll())
                .then(userRepository.deleteAll())
                .block();
    }

    @Test
    @DisplayName("Успешное получение всех событий модератором")
    public void testGetAllEventsAsModeratorSuccess() {
        // Создаём записи в таблице files
        FileEntity file1 = new FileEntity();
        file1.setFileName("file1.txt");
        file1.setLocation("path/to/file1"); // Задаём значение для поля location
        file1 = fileRepository.save(file1).block();

        FileEntity file2 = new FileEntity();
        file2.setFileName("file2.txt");
        file2.setLocation("path/to/file2"); // Задаём значение для поля location
        file2 = fileRepository.save(file2).block();

        // Проверяем, что файлы сохранены
        assertNotNull(file1, "file1 не был сохранён");
        assertNotNull(file2, "file2 не был сохранён");
        assertNotNull(file1.getId(), "ID file1 не должен быть null");
        assertNotNull(file2.getId(), "ID file2 не должен быть null");

        // Создаём пользователей
        UserEntity user1 = new UserEntity();
        user1.setUsername("user1");
        user1.setPassword("password1");
        user1.setFirstName("John");
        user1.setLastName("Doe");
        user1.setRole(UserRole.USER);
        user1.setStatus(Status.ACTIVE);
        user1 = userRepository.save(user1).block();

        UserEntity user2 = new UserEntity();
        user2.setUsername("user2");
        user2.setPassword("password2");
        user2.setFirstName("Jane");
        user2.setLastName("Smith");
        user2.setRole(UserRole.USER);
        user2.setStatus(Status.ACTIVE);
        user2 = userRepository.save(user2).block();

        // Проверяем, что пользователи сохранены
        assertNotNull(user1, "user1 не был сохранён");
        assertNotNull(user2, "user2 не был сохранён");
        assertNotNull(user1.getId(), "ID user1 не должен быть null");
        assertNotNull(user2.getId(), "ID user2 не должен быть null");

        // Создаём события, связанные с пользователями
        EventEntity event1 = new EventEntity();
        event1.setFileId(file1.getId());
        event1.setUserId(user1.getId());
        event1.setStatus(Status.ACTIVE);
        event1 = eventRepository.save(event1).block();

        EventEntity event2 = new EventEntity();
        event2.setFileId(file2.getId());
        event2.setUserId(user2.getId());
        event2.setStatus(Status.ACTIVE);
        event2 = eventRepository.save(event2).block();

        // Проверяем, что события сохранены
        assertNotNull(event1, "event1 не был сохранён");
        assertNotNull(event2, "event2 не был сохранён");
        assertEquals(file1.getId(), event1.getFileId(), "fileId в event1 не совпадает с file1.getId()");
        assertEquals(file2.getId(), event2.getFileId(), "fileId в event2 не совпадает с file2.getId()");

        // Выполняем запрос и проверяем результат
        FileEntity finalFile = file1;
        FileEntity finalFile1 = file1;
        FileEntity finalFile2 = file2;
        FileEntity finalFile3 = file2;
        webTestClient.mutateWith(mockUser().roles("MODERATOR"))
                .get()
                .uri("/api/v1/events")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EventEntity.class)
                .hasSize(2)
                .consumeWith(response -> {
                    assertTrue(response.getResponseBody().stream()
                                    .anyMatch(e -> e.getFileId().equals(finalFile.getId())),
                            "Event с fileId = " + finalFile1.getId() + " не найден");
                    assertTrue(response.getResponseBody().stream()
                                    .anyMatch(e -> e.getFileId().equals(finalFile2.getId())),
                            "Event с fileId = " + finalFile3.getId() + " не найден");
                });
    }

    @Test
    @DisplayName("Получение всех событий запрещено для неавторизованного пользователя")
    public void testGetAllEventsAsUserForbidden() {
        webTestClient.mutateWith(mockUser().roles("USER"))
                .get()
                .uri("/api/v1/events")
                .exchange()
                .expectStatus().isForbidden();
    }
}

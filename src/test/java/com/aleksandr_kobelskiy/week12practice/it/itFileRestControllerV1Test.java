package com.aleksandr_kobelskiy.week12practice.it;

import com.aleksandr_kobelskiy.week12practice.config.AwsS3TestConfig;
import com.aleksandr_kobelskiy.week12practice.config.MySqlTestcontainerConfig;
import com.aleksandr_kobelskiy.week12practice.entity.FileEntity;
import com.aleksandr_kobelskiy.week12practice.entity.Status;
import com.aleksandr_kobelskiy.week12practice.entity.UserEntity;
import com.aleksandr_kobelskiy.week12practice.repository.FileRepository;
import com.aleksandr_kobelskiy.week12practice.service.UserService;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import({MySqlTestcontainerConfig.class, AwsS3TestConfig.class})
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@TestPropertySource(locations = "classpath:application-test.properties")
public class itFileRestControllerV1Test {

    @Autowired
    private S3AsyncClient s3AsyncClient;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private UserService userService;

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
        // Настройка WebTestClient с увеличенным тайм-аутом
        webTestClient = webTestClient
                .mutate()
                .responseTimeout(Duration.ofSeconds(30)) // Увеличение тайм-аута
                .build();

        // Настройка мока для UserService
        UserEntity mockUser = new UserEntity();
        mockUser.setId(1L);
        mockUser.setUsername("testUser");

        Mockito.when(userService.getCurrentUser()).thenReturn(Mono.just(mockUser));

        // Убедитесь, что бакет существует
        createBucketIfNotExists("test-bucket");

        // Очистка бакета от файлов
        clearBucket("test-bucket");

//        fileRepository.deleteAll().block();
    }

    private void clearBucket(String bucketName) {
        s3AsyncClient.listObjectsV2(request -> request.bucket(bucketName))
                .join()
                .contents()
                .forEach(object -> s3AsyncClient.deleteObject(deleteRequest -> deleteRequest.bucket(bucketName).key(object.key()))
                        .join());
    }

    /**
     * Создаёт бакет, если он ещё не существует.
     */
    private void createBucketIfNotExists(String bucketName) {
        CompletableFuture<Boolean> bucketExistsFuture = s3AsyncClient.listBuckets()
                .thenApply(response -> response.buckets().stream()
                        .anyMatch(bucket -> bucket.name().equals(bucketName)));

        boolean bucketExists = bucketExistsFuture.join();

        if (!bucketExists) {
            CompletableFuture<CreateBucketResponse> bucketCreation = s3AsyncClient.createBucket(CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
            bucketCreation.join(); // Дожидаемся завершения создания бакета

            // Убедитесь, что бакет действительно доступен
            Awaitility.await()
                    .atMost(Duration.ofSeconds(10))
                    .until(() -> s3AsyncClient.listBuckets().join()
                            .buckets().stream()
                            .anyMatch(bucket -> bucket.name().equals(bucketName)));
        }
    }

    @Test
    @DisplayName("Проверка создания бакета в LocalStack")
    public void testCreateBucket() {
        String bucketName = "test-bucket";

        s3AsyncClient.createBucket(CreateBucketRequest.builder().bucket(bucketName).build()).join();

        boolean bucketExists = s3AsyncClient.listBuckets().join()
                .buckets()
                .stream()
                .anyMatch(bucket -> bucket.name().equals(bucketName));

        assertTrue(bucketExists, "Bucket should exist in LocalStack");
    }

    @Test
    @DisplayName("Успешное получение всех местоположений файлов")
    public void testGetAllFileLocationsSuccess() {
        FileEntity file = new FileEntity();
        file.setFileName("test.txt");
        file.setLocation("path/to/test.txt");
        file.setStatus(Status.ACTIVE);
        fileRepository.save(file).block();

        webTestClient.mutateWith(mockUser().roles("MODERATOR"))
                .get()
                .uri("/api/v1/files/locations")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(String.class)
                .value(locations -> {
                    assertNotNull(locations);
                    assertNotNull(locations.getFirst());
                });
    }

    @Test
    @DisplayName("Неуспешное получение всех местоположений файлов (пустой список)")
    public void testGetAllFileLocationsEmpty() {
        // Выполняем запрос
        webTestClient.mutateWith(mockUser().roles("MODERATOR"))
                .get()
                .uri("/api/v1/files/locations")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(List.class)
                .consumeWith(response -> {
                    List<String> locations = response.getResponseBody();
                    assertNotNull(locations, "Ответ не должен быть null");
                    assertEquals(1, locations.size(), "Список должен содержать одно сообщение");
                    assertTrue(locations.contains("File location links not found"),
                            "Ответ должен содержать сообщение 'File location links not found'");
                });
    }

    @Test
    @DisplayName("Проверка загрузки файла через LocalStack")
    public void testUploadFile() throws IOException {
        // Имя бакета и файл для загрузки
        String bucketName = "test-bucket";
        String fileName = "test-file.txt";
        String fileContent = "This is a test file content.";
        String personalFolder = "akobelskiy-test"; // Используйте значение из application-test.properties

        // Создаем бакет, если он не существует
        createBucketIfNotExists(bucketName);

        // Логируем проверку наличия бакета
        boolean bucketExists = s3AsyncClient.listBuckets().join()
                .buckets()
                .stream()
                .anyMatch(bucket -> bucket.name().equals(bucketName));
        System.out.println("Бакет существует после создания: " + bucketExists);

        // Создаем mock файл для загрузки
        Path tempFile = Files.createTempFile(fileName, ".txt");
        Files.writeString(tempFile, fileContent);

        MockMultipartFile mockMultipartFile = new MockMultipartFile(
                "file",
                fileName,
                MediaType.TEXT_PLAIN_VALUE,
                Files.readAllBytes(tempFile)
        );

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", mockMultipartFile.getResource());

        // Выполняем запрос с имитацией аутентифицированного пользователя
        webTestClient.mutateWith(mockUser().roles("MODERATOR")) // Указываем роль пользователя
                .post()
                .uri("/api/v1/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String responseBody = response.getResponseBody();
                    assertNotNull(responseBody);
                    assertTrue(responseBody.contains("File uploaded successfully"), "Ответ должен содержать сообщение о загрузке файла");
                });

        // Логируем содержимое бакета
        System.out.println("Проверяем наличие файла в S3...");
        s3AsyncClient.listObjectsV2(request -> request.bucket(bucketName).prefix(personalFolder + "/"))
                .join()
                .contents()
                .forEach(object -> System.out.println("Файл в S3: " + object.key()));

        // Проверяем, что файл был загружен в S3
        boolean fileExists = s3AsyncClient.listObjectsV2(request -> request.bucket(bucketName).prefix(personalFolder + "/" + fileName))
                .join()
                .contents()
                .stream()
                .anyMatch(object -> object.key().equals(personalFolder + "/" + fileName));

        assertTrue(fileExists, "Файл должен быть загружен в S3");

        // Удаляем временный файл
        Files.deleteIfExists(tempFile);
    }

    @Test
    @DisplayName("Неуспешная загрузка файла на Localstack (ошибка сервера)")
    public void testUploadFileServerError() {
        webTestClient.mutateWith(mockUser().roles("MODERATOR"))
                .post()
                .uri("/api/v1/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertNotNull(body, "Ответ не должен быть null");
                    assertTrue(body.contains("No multipart boundary found in Content-Type"),
                            "Ответ должен содержать сообщение 'No multipart boundary found in Content-Type'");
                });
    }

    @Test
    @DisplayName("Успешное получение файла по ID")
    public void testGetFileByIdSuccess() {
        FileEntity file = new FileEntity();
        file.setFileName("test.txt");
        file.setLocation("path/to/test.txt");
        file.setStatus(Status.ACTIVE);
        file = fileRepository.save(file).block();

        webTestClient.mutateWith(mockUser().roles("MODERATOR"))
                .get()
                .uri("/api/v1/files/" + file.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertEquals("path/to/test.txt", body, "Ответ должен содержать правильное расположение файла"));
    }

    @Test
    @DisplayName("Неуспешное получение файла по ID (файл не найден)")
    public void testGetFileByIdNotFound() {
        webTestClient.mutateWith(mockUser().roles("MODERATOR"))
                .get()
                .uri("/api/v1/files/999")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .value(body -> assertTrue(body.contains("File not found"), "Ответ должен содержать сообщение 'File not found'"));
    }

    @Test
    @DisplayName("Успешное удаление файла")
    public void testDeleteFileSuccess() {
        FileEntity file = new FileEntity();
        file.setFileName("test.txt");
        file.setLocation("path/to/test.txt");
        file.setStatus(Status.ACTIVE);
        file = fileRepository.save(file).block();

        webTestClient.mutateWith(mockUser().roles("MODERATOR"))
                .delete()
                .uri("/api/v1/files/" + file.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertTrue(body.contains("File successfully deleted"),
                        "Ответ должен содержать сообщение 'File successfully deleted'"));
    }

    @Test
    @DisplayName("Неуспешное удаление файла (файл не найден)")
    public void testDeleteFileNotFound() {
        webTestClient.mutateWith(mockUser().roles("MODERATOR"))
                .delete()
                .uri("/api/v1/files/999")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .value(body -> Assertions.assertTrue(body.contains("File with this id not found"),
                        "Ответ должен содержать сообщение 'File with this id not found'"));
    }

    @Test
    @DisplayName("Успешное обновление файла")
    public void testUpdateFileSuccess() throws IOException {
        // Создание файла в базе данных
        FileEntity file = new FileEntity();
        file.setFileName("test.txt");
        file.setLocation("path/to/test.txt");
        file.setStatus(Status.ACTIVE);
        file = fileRepository.save(file).block();

        // Создаем mock файл для загрузки
        String newFileName = "new-test.txt";
        Path tempFile = Files.createTempFile(newFileName, ".txt");
        Files.writeString(tempFile, "Updated content for the file");

        MockMultipartFile mockMultipartFile = new MockMultipartFile(
                "file",
                newFileName,
                MediaType.TEXT_PLAIN_VALUE,
                Files.readAllBytes(tempFile)
        );

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", mockMultipartFile.getResource());

        // Выполняем запрос на обновление файла
        webTestClient.mutateWith(mockUser().roles("MODERATOR"))
                .put()
                .uri("/api/v1/files/" + file.getId())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertTrue(body.contains("File successfully updated"),
                        "Ответ должен содержать сообщение 'File successfully updated'"));

        // Удаляем временный файл
        Files.deleteIfExists(tempFile);
    }

    @Test
    @DisplayName("Неуспешное обновление файла (ошибка при удалении)")
    public void testUpdateFileDeleteError() {
        webTestClient.mutateWith(mockUser().roles("MODERATOR"))
                .put()
                .uri("/api/v1/files/999")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(String.class)
                .value(response -> Assertions.assertTrue(response.contains("Failed to update file: could not delete existing file"),
                        "Ответ должен содержать сообщение 'Failed to update file: could not delete existing file'"));
    }
}

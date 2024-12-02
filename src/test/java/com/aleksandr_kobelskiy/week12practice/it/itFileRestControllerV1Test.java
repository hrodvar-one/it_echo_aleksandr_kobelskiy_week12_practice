package com.aleksandr_kobelskiy.week12practice.it;

import com.aleksandr_kobelskiy.week12practice.config.MySqlTestcontainerConfig;
import com.aleksandr_kobelskiy.week12practice.entity.FileEntity;
import com.aleksandr_kobelskiy.week12practice.entity.Status;
import com.aleksandr_kobelskiy.week12practice.repository.FileRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(MySqlTestcontainerConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@ActiveProfiles("test")
public class itFileRestControllerV1Test {

    private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:latest");
    private static LocalStackContainer localStackContainer;

    @Autowired
    private S3AsyncClient s3AsyncClient; // Внедряем S3AsyncClient для профиля test

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

    @BeforeAll
    public static void startLocalStack() {
        localStackContainer = new LocalStackContainer(LOCALSTACK_IMAGE)
                .withServices(LocalStackContainer.Service.S3);
        localStackContainer.start();
    }

    @BeforeEach
    public void setUp() {
        // Настройка WebTestClient с увеличенным тайм-аутом
        webTestClient = webTestClient
                .mutate()
                .responseTimeout(Duration.ofSeconds(30)) // Увеличение тайм-аута
                .build();

        // Очистка базы данных
        cleanDatabase();

        // Инициализация клиента S3 и создание тестового бакета
        initializeS3Client();
    }

    @AfterAll
    public static void stopLocalStack() {
        if (localStackContainer != null) {
            localStackContainer.stop();
        }
    }

    @AfterEach
    public void cleanupS3() {
        String bucketName = "akobelskiy";
        s3AsyncClient.listObjectsV2(b -> b.bucket(bucketName))
                .thenCompose(response -> CompletableFuture.allOf(response.contents().stream()
                        .map(object -> s3AsyncClient.deleteObject(d -> d.bucket(bucketName).key(object.key())))
                        .toArray(CompletableFuture[]::new)))
                .thenCompose(v -> s3AsyncClient.deleteBucket(b -> b.bucket(bucketName)))
                .join(); // Ждём завершения всех операций
    }

    /**
     * Очищает базу данных перед каждым тестом.
     */
    private void cleanDatabase() {
        fileRepository.deleteAll().block();
    }

    /**
     * Инициализирует асинхронный клиент S3 и создаёт тестовый бакет.
     */
    private void initializeS3Client() {
        String bucketName = "akobelskiy";

        // Проверяем, существует ли бакет
        CompletableFuture<Boolean> bucketExistsFuture = s3AsyncClient.listBuckets()
                .thenApply(response -> response.buckets().stream()
                        .anyMatch(bucket -> bucket.name().equals(bucketName)));

        boolean bucketExists = bucketExistsFuture.join();

        // Создаём бакет, только если он не существует
        if (!bucketExists) {
            CompletableFuture<CreateBucketResponse> bucketCreation = s3AsyncClient.createBucket(CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build());

            bucketCreation.join(); // Блокируем поток до завершения создания бакета
        }

//        String bucketName = "akobelskiy";
//
//        // Создайте бакет, если он не существует
//        s3AsyncClient.createBucket(CreateBucketRequest.builder()
//                        .bucket(bucketName)
//                        .build())
//                .join();
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

//    @Test
//    @DisplayName("Неуспешное получение всех местоположений файлов (пустой список)")
//    public void testGetAllFileLocationsEmpty() {
//        webTestClient.mutateWith(mockUser().roles("MODERATOR"))
//                .get()
//                .uri("/api/v1/files/locations")
//                .exchange()
//                .expectStatus().isOk()
//                .expectBodyList(String.class)
//                .value(locations -> {
//                    assertNotNull(locations, "Список местоположений не должен быть null");
//                    assertEquals(List.of("File location links not found"),
//                            List.copyOf(locations),
//                            "Ответ должен быть списком с единственным элементом 'File location links not found'");
//                });
//    }

//    @Test
//    @DisplayName("Успешная загрузка файла")
//    public void testUploadFileSuccess() {
//        // Убедимся, что LocalStack работает и бакет существует
////        String bucketName = "test-bucket";
//        String bucketName = "akobelskiy";
//        String fileName = "test-file.txt";
//        String fileContent = "This is a test file";
//
//        // Загружаем файл через контроллер
//        webTestClient.mutateWith(mockUser().roles("MODERATOR"))
//                .post()
//                .uri("/api/v1/files")
//                .contentType(MediaType.MULTIPART_FORM_DATA)
//                .body(BodyInserters.fromMultipartData("file", fileContent)
//                        .with("filename", fileName))
//                .exchange()
//                .expectStatus().isOk()
//                .expectBody(String.class)
//                .value(response -> assertTrue(response.contains("File uploaded successfully"),
//                        "Response should indicate successful upload"));
//
//        // Получаем список объектов в бакете
//        CompletableFuture<Boolean> fileExistsFuture = s3AsyncClient.listObjectsV2(b -> b.bucket(bucketName))
//                .thenApply(response -> response.contents().stream()
//                        .anyMatch(object -> object.key().equals(fileName)));
//
//        // Проверяем, что файл действительно существует
//        assertTrue(fileExistsFuture.join(), "Uploaded file should exist in S3 bucket");
//    }

    @Test
    @DisplayName("Успешная загрузка файла")
    public void testUploadFileSuccess() throws IOException {
        // Убедимся, что LocalStack работает и бакет существует
        String bucketName = "akobelskiy";
        String randomFileName = "test-file-" + UUID.randomUUID() + ".txt"; // Уникальное имя файла
        String fileContent = "This is a test file";

        // Создаем временный файл для загрузки
        Path tempFile = Files.createTempFile(randomFileName, null);
        Files.writeString(tempFile, fileContent);

        // Загружаем файл через контроллер
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", tempFile.toFile())
                .header("Content-Disposition", "form-data; name=file; filename=" + randomFileName);

        webTestClient.mutateWith(mockUser().roles("ADMIN"))
    //    webTestClient.mutateWith(mockUser("testUser").roles("MODERATOR"))
                .post()
                .uri("/api/v1/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(response -> assertTrue(response.contains("File uploaded successfully"),
                        "Response should indicate successful upload"));

        // Получаем список объектов в бакете
        CompletableFuture<Boolean> fileExistsFuture = s3AsyncClient.listObjectsV2(b -> b.bucket(bucketName))
                .thenApply(response -> response.contents().stream()
                        .anyMatch(object -> object.key().equals(randomFileName))); // Проверяем уникальное имя

        // Проверяем, что файл действительно существует
        assertTrue(fileExistsFuture.join(), "Uploaded file should exist in S3 bucket");

        // Удаляем временный файл
        Files.deleteIfExists(tempFile);
    }

//    @Test
//    @DisplayName("Неуспешная загрузка файла (ошибка сервера)")
//    public void testUploadFileServerError() {
//        webTestClient.post()
//                .uri("/api/v1/files")
//                .contentType(MediaType.MULTIPART_FORM_DATA)
//                .exchange()
//                .expectStatus().is5xxServerError()
//                .expectBody(String.class)
//                .value(body -> assert body.contains("An error occurred"));
//    }
//
//    @Test
//    @DisplayName("Успешное получение файла по ID")
//    public void testGetFileByIdSuccess() {
//        FileEntity file = new FileEntity();
//        file.setFileName("test.txt");
//        file.setLocation("path/to/test.txt");
//        file.setStatus(Status.ACTIVE);
//        file = fileRepository.save(file).block();
//
//        webTestClient.mutateWith(mockUser().roles("MODERATOR"))
//                .get()
//                .uri("/api/v1/files/" + file.getId())
//                .exchange()
//                .expectStatus().isOk()
//                .expectBody(String.class)
//                .value(body -> assert body.equals("path/to/test.txt"));
//    }
//
//    @Test
//    @DisplayName("Неуспешное получение файла по ID (файл не найден)")
//    public void testGetFileByIdNotFound() {
//        webTestClient.mutateWith(mockUser().roles("MODERATOR"))
//                .get()
//                .uri("/api/v1/files/999")
//                .exchange()
//                .expectStatus().isNotFound()
//                .expectBody(String.class)
//                .value(body -> assert body.contains("File not found"));
//    }
//
//    @Test
//    @DisplayName("Успешное удаление файла")
//    public void testDeleteFileSuccess() {
//        FileEntity file = new FileEntity();
//        file.setFileName("test.txt");
//        file.setLocation("path/to/test.txt");
//        file.setStatus(Status.ACTIVE);
//        file = fileRepository.save(file).block();
//
//        webTestClient.mutateWith(mockUser().roles("MODERATOR"))
//                .delete()
//                .uri("/api/v1/files/" + file.getId())
//                .exchange()
//                .expectStatus().isOk()
//                .expectBody(String.class)
//                .value(body -> assert body.contains("File successfully deleted"));
//    }
//
//    @Test
//    @DisplayName("Неуспешное удаление файла (файл не найден)")
//    public void testDeleteFileNotFound() {
//        webTestClient.mutateWith(mockUser().roles("MODERATOR"))
//                .delete()
//                .uri("/api/v1/files/999")
//                .exchange()
//                .expectStatus().isNotFound()
//                .expectBody(String.class)
//                .value(body -> assert body.contains("File not found"));
//    }
//
//    @Test
//    @DisplayName("Успешное обновление файла")
//    public void testUpdateFileSuccess() {
//        FileEntity file = new FileEntity();
//        file.setFileName("test.txt");
//        file.setLocation("path/to/test.txt");
//        file.setStatus(Status.ACTIVE);
//        file = fileRepository.save(file).block();
//
//        webTestClient.mutateWith(mockUser().roles("MODERATOR"))
//                .put()
//                .uri("/api/v1/files/" + file.getId())
//                .contentType(MediaType.MULTIPART_FORM_DATA)
//                .bodyValue(Mono.just("newMockFile"))
//                .exchange()
//                .expectStatus().isOk()
//                .expectBody(String.class)
//                .value(body -> assert body.contains("File successfully updated"));
//    }
//
//    @Test
//    @DisplayName("Неуспешное обновление файла (ошибка при удалении)")
//    public void testUpdateFileDeleteError() {
//        webTestClient.mutateWith(mockUser().roles("MODERATOR"))
//                .put()
//                .uri("/api/v1/files/999")
//                .contentType(MediaType.MULTIPART_FORM_DATA)
//                .exchange()
//                .expectStatus().is5xxServerError()
//                .expectBody(String.class)
//                .value(body -> assert body.contains("Failed to update file: could not delete existing file"));
//    }
}

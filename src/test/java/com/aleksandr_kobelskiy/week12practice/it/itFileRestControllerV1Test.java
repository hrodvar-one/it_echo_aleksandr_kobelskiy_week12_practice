package com.aleksandr_kobelskiy.week12practice.it;

import com.aleksandr_kobelskiy.week12practice.config.AwsS3Config;
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
import org.springframework.context.annotation.Profile;
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
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
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
//@Import(MySqlTestcontainerConfig.class)
@Import({MySqlTestcontainerConfig.class, AwsS3TestConfig.class})
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@TestPropertySource(locations = "classpath:application-test.properties")
public class itFileRestControllerV1Test {

    @Autowired
    private S3AsyncClient s3AsyncClient;

//    @Autowired
//    private FileRepository fileRepository;

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

//        fileRepository.deleteAll().block();
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

//    @Test   рабочий тест
//    @DisplayName("Успешное получение всех местоположений файлов")
//    public void testGetAllFileLocationsSuccess() {
//        FileEntity file = new FileEntity();
//        file.setFileName("test.txt");
//        file.setLocation("path/to/test.txt");
//        file.setStatus(Status.ACTIVE);
//        fileRepository.save(file).block();
//
//        webTestClient.mutateWith(mockUser().roles("MODERATOR"))
//                .get()
//                .uri("/api/v1/files/locations")
//                .exchange()
//                .expectStatus().isOk()
//                .expectBodyList(String.class)
//                .value(locations -> {
//                    assertNotNull(locations);
//                    assertNotNull(locations.getFirst());
//                });
//    }

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


    @Test
    @DisplayName("Простая запись файла в LocalStack S3 не через контроллер")
    public void testWriteFileToLocalStack() throws IOException {
        // Имя бакета и имя файла
        String bucketName = "test-bucket";
        String fileName = "test-file.txt";
        String fileContent = "This is a test file";

        // Создаем бакет, если он не существует
        createBucketIfNotExists(bucketName);

        // Проверяем, что бакет создан
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .until(() -> s3AsyncClient.listBuckets().join()
                        .buckets().stream()
                        .anyMatch(bucket -> bucket.name().equals(bucketName)));

        // Создаем временный файл
        Path tempFile = Files.createTempFile("temp-", ".txt");
        Files.writeString(tempFile, fileContent);

        // Загружаем файл в S3
        s3AsyncClient.putObject(
                b -> b.bucket(bucketName).key(fileName),
                tempFile
        ).join();

        // Проверяем, что файл существует в бакете
        boolean fileExists = s3AsyncClient.listObjectsV2(b -> b.bucket(bucketName))
                .join()
                .contents()
                .stream()
                .anyMatch(object -> object.key().equals(fileName));

        assertTrue(fileExists, "Файл должен существовать в бакете");

        // Удаляем временный файл
        Files.deleteIfExists(tempFile);
    }


//    ///////////////////////////////////////////////////////////////////
//    @Test
//    @DisplayName("Успешная загрузка файла")
//    public void testUploadFileSuccess() throws IOException {
//        // Название бакета
//        String bucketName = "akobelskiy-test";
//        String randomFileName = "test-file-" + UUID.randomUUID() + ".txt"; // Уникальное имя файла
//        String fileContent = "This is a test file";
//
//        // Создаем бакет, если он отсутствует
//        createBucketIfNotExists(bucketName);
//
//        // Убеждаемся, что бакет существует
//        Awaitility.await()
//                .atMost(Duration.ofSeconds(10))
//                .until(() -> s3AsyncClient.listBuckets().join()
//                        .buckets().stream()
//                        .anyMatch(bucket -> bucket.name().equals(bucketName)));
//
//        // Создаем временный файл для загрузки
//        Path tempFile = Files.createTempFile(randomFileName, null);
//        Files.writeString(tempFile, fileContent);
//
//        // Загружаем файл через контроллер
//        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
//        bodyBuilder.part("file", tempFile.toFile())
//                .header("Content-Disposition", "form-data; name=file; filename=" + randomFileName);
//
//        webTestClient.mutateWith(mockUser().roles("ADMIN"))
//    //    webTestClient.mutateWith(mockUser("testUser").roles("MODERATOR"))
//                .post()
//                .uri("/api/v1/files")
//                .contentType(MediaType.MULTIPART_FORM_DATA)
//                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
//                .exchange()
//                .expectStatus().isOk()
//                .expectBody(String.class)
//                .value(response -> assertTrue(response.contains("File uploaded successfully"),
//                        "Response should indicate successful upload"));
//
//        // Проверяем, что файл существует в бакете
//        Awaitility.await()
//                .atMost(Duration.ofSeconds(10))
//                .until(() -> s3AsyncClient.listObjectsV2(b -> b.bucket(bucketName))
//                        .join().contents().stream()
//                        .anyMatch(object -> object.key().equals(randomFileName)));
//
//        // Удаляем временный файл
//        Files.deleteIfExists(tempFile);
//    }
// ///////////////////////////////////////////////////////////////

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

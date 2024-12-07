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
}

package com.aleksandr_kobelskiy.week12practice.repository.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class AwsS3Repository {

    private final S3AsyncClient s3AsyncClient;

    @Value("${aws.s3.bucketName}")
    private String bucketName;

    @Value("${aws.s3.personalFolderName}")
    private String personalFolderName;

    @Value("${aws.s3.endpointOverride}")
    private String endpointOverride;

    public Mono<String> uploadFile(FilePart filePart) {
        return ensureFolderExists(personalFolderName) // Убедимся, что папка существует
                .then(checkFileExistsAndUpload(filePart, personalFolderName)); // Проверяем и сохраняем файл
    }

    private Mono<Void> ensureFolderExists(String folderName) {
        return Mono.fromFuture(
                s3AsyncClient.listObjectsV2(
                        ListObjectsV2Request.builder()
                                .bucket(bucketName)
                                .prefix(folderName + "/") // Проверяем, есть ли префикс
                                .maxKeys(1) // Проверим только один объект
                                .build()
                )
        ).flatMap(response -> {
            if (response.contents().isEmpty()) {
                // Если папки нет, создаём "пустой объект" с этим префиксом
                return Mono.fromFuture(
                        s3AsyncClient.putObject(
                                PutObjectRequest.builder()
                                        .bucket(bucketName)
                                        .key(folderName + "/") // Пустой объект для папки
                                        .build(),
                                AsyncRequestBody.empty()
                        )
                ).then();
            }
            return Mono.empty(); // Папка уже существует
        });
    }

    private Mono<String> checkFileExistsAndUpload(FilePart filePart, String folderName) {
        String key = folderName + "/" + filePart.filename(); // Формируем путь без UUID
        return fileExists(key).flatMap(exists -> {
            if (exists) {
                // Если файл существует, выбрасываем исключение
                return Mono.error(new RuntimeException("File with name '" + filePart.filename() + "' already exists in folder '" + folderName + "'"));
            } else {
                // Если файла нет, загружаем его
                return uploadFileToS3(filePart, key);
            }
        });
    }

    private Mono<Boolean> fileExists(String key) {
        return Mono.fromFuture(
                s3AsyncClient.listObjectsV2(
                        ListObjectsV2Request.builder()
                                .bucket(bucketName)
                                .prefix(key) // Проверяем конкретный ключ
                                .maxKeys(1) // Проверим только один объект
                                .build()
                )
        ).map(response -> !response.contents().isEmpty()); // Если список не пуст, файл существует
    }

    private Mono<String> uploadFileToS3(FilePart filePart, String key) {
        return filePart.content()
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    return ByteBuffer.wrap(bytes);
                })
                .reduce((buffer1, buffer2) -> {
                    ByteBuffer combined = ByteBuffer.allocate(buffer1.remaining() + buffer2.remaining());
                    combined.put(buffer1);
                    combined.put(buffer2);
                    combined.flip();
                    return combined;
                })
                .flatMap(buffer -> {
                    CompletableFuture<PutObjectResponse> future = s3AsyncClient.putObject(
                            PutObjectRequest.builder()
                                    .bucket(bucketName)
                                    .key(key)
                                    .build(),
                            AsyncRequestBody.fromByteBuffer(buffer)
                    );

                    return Mono.fromFuture(future)
                            .map(response -> {
                                if (!response.sdkHttpResponse().isSuccessful()) {
                                    throw new RuntimeException("Failed to upload file to S3: " + response.sdkHttpResponse().statusCode());
                                }
//                                return "s3://" + bucketName + "/" + key; // Возвращаем URL-адрес файла
                                return endpointOverride + "/" + bucketName + "/" + key; // Возвращаем URL-адрес файла
                            });
                });
    }


    public Flux<String> listFilesInPersonalFolder() {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(personalFolderName + "/") // Указываем префикс для папки
                .build();

        CompletableFuture<List<String>> future = s3AsyncClient.listObjectsV2(request)
                .thenApply(response -> response.contents().stream()
                        .map(S3Object::key)
                        .filter(key -> !key.endsWith("/"))
                        .map(key -> endpointOverride + "/" + bucketName + "/" + key)
                        .toList());

        // Преобразуем CompletableFuture в Mono, а затем развернем List<String> в Flux<String>
        return Mono.fromFuture(future) // Преобразуем CompletableFuture в Mono<List<String>>
                .flatMapMany(Flux::fromIterable); // Разворачиваем List<String> в Flux<String>
    }


    public Flux<String> deleteAllFilesInPersonalFolder() {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(personalFolderName + "/") // Указываем префикс для папки
                .build();

        CompletableFuture<List<String>> future = s3AsyncClient.listObjectsV2(request)
                .thenApply(response -> response.contents().stream()
                        .map(S3Object::key)
                        .toList());

        // Преобразуем CompletableFuture в Mono<List<String>>
        return Mono.fromFuture(future)
                .flatMapMany(Flux::fromIterable) // Преобразуем List<String> в Flux<String>
                .flatMap(fileKey -> deleteFile(fileKey)
                        .thenReturn(fileKey)); // Удаляем файл и возвращаем его имя
    }


    private Mono<Void> deleteFile(String fileKey) {
        return Mono.fromFuture(s3AsyncClient.deleteObject(builder -> builder
                        .bucket(bucketName)
                        .key(fileKey)
                        .build()))
                .then(); // Преобразует Mono<DeleteObjectResponse> в Mono<Void>
    }
}

package com.aleksandr_kobelskiy.week12practice.service;

import com.aleksandr_kobelskiy.week12practice.entity.EventEntity;
import com.aleksandr_kobelskiy.week12practice.entity.FileEntity;
import com.aleksandr_kobelskiy.week12practice.entity.Status;
import com.aleksandr_kobelskiy.week12practice.repository.EventRepository;
import com.aleksandr_kobelskiy.week12practice.repository.FileRepository;
import com.aleksandr_kobelskiy.week12practice.repository.s3.AwsS3Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class FileService {

    private final AwsS3Repository awsS3Repository;
    private final FileRepository fileRepository;
    private final EventService eventService;
    private final EventRepository eventRepository;
    private final UserService userService;

    public Flux<String> getAllFileLocationsFromDB() {
        return fileRepository.findByStatus(Status.ACTIVE) // Получаем только записи со статусом ACTIVE
                .map(FileEntity::getLocation); // Преобразуем их в список путей (location)
    }


    public Mono<String> uploadAndSaveFile(FilePart filePart) {
        return awsS3Repository.uploadFile(filePart)
                .flatMap(location -> {
                    FileEntity fileEntity = new FileEntity();
                    fileEntity.setFileName(filePart.filename());
                    fileEntity.setLocation(location);

                    return fileRepository.save(fileEntity)
                            .flatMap(savedFile ->
                                    eventService.logEvent(savedFile.getId(), Status.ACTIVE) // Передаём только fileId
                                            .thenReturn(location));
                });
    }


    public Mono<Void> deleteFile(Long fileId) {
        return fileRepository.findByIdAndStatus(fileId, Status.ACTIVE)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("File with this id not found")))
                .flatMap(file -> {
                    String fileKey = extractFileKey(file.getLocation());
                    return awsS3Repository.deleteFileFromS3(fileKey)
                            .then(fileRepository.save(file.toBuilder().status(Status.DELETED).build()))
                            .then(eventRepository.findByFileId(fileId).next()
                                    .flatMap(existingEvent -> userService.getCurrentUser()
                                            .flatMap(currentUser -> {
                                                EventEntity newEvent = EventEntity.builder()
                                                        .userId(currentUser.getId())
                                                        .fileId(fileId)
                                                        .status(Status.DELETED)
                                                        .build();
                                                return eventRepository.save(newEvent);
                                            }))
                                    .switchIfEmpty(userService.getCurrentUser()
                                            .flatMap(currentUser -> {
                                                EventEntity newEvent = EventEntity.builder()
                                                        .userId(currentUser.getId())
                                                        .fileId(fileId)
                                                        .status(Status.DELETED)
                                                        .build();
                                                return eventRepository.save(newEvent);
                                            }))
                                    .then());
                });
    }


    public Flux<String> getFilesInPersonalFolder() {
        return awsS3Repository.listFilesInPersonalFolder();
    }


    public Flux<String> deleteFilesInPersonalFolder() {
        return awsS3Repository.deleteAllFilesInPersonalFolder();
    }


    private String extractFileKey(String fileUrl) {
        // Удаляем базовый URL (например, https://storage.yandexcloud.net/javateam1/)
        return fileUrl.replace("https://storage.yandexcloud.net/javateam1/", "");
    }
}

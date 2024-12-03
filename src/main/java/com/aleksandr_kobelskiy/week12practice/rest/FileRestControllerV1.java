package com.aleksandr_kobelskiy.week12practice.rest;

import com.aleksandr_kobelskiy.week12practice.entity.Status;
import com.aleksandr_kobelskiy.week12practice.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class FileRestControllerV1 {

    private final FileService fileService;

    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @GetMapping(value = "/locations", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<String>> getAllFileLocations() {
        return fileService.getAllFileLocationsFromDB()
                .collectList() // Собираем Flux в список
                .map(locations -> {
                    if (locations.isEmpty()) {
                        // Если список пуст, возвращаем сообщение
                        return List.of("File location links not found");
                    } else {
                        // Если записи найдены, возвращаем список ссылок
                        return locations;
                    }
                });
    }


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<String>> uploadFile(@RequestPart("file") Mono<FilePart> file) {
        return file.flatMap(filePart -> fileService.uploadAndSaveFile(filePart))
                .map(location -> ResponseEntity.ok("File uploaded successfully: " + location))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage())));
    }


    @GetMapping(value = "/personal-folder", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<String> listFilesInPersonalFolder() {
        return fileService.getFilesInPersonalFolder()
                .switchIfEmpty(Flux.just("The directory is empty")); // Если Flux пустой, возвращаем сообщение
    }

    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @DeleteMapping("/{fileId}")
    public Mono<ResponseEntity<String>> deleteFile(@PathVariable("fileId") Long fileId) {
        return fileService.deleteFile(fileId)
                .thenReturn(ResponseEntity.ok("File successfully deleted"))
                .onErrorResume(e -> {
                    if (e instanceof IllegalArgumentException) {
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()));
                    }
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred"));
                });
    }


    @DeleteMapping("/personal-folder")
    public Flux<String> deleteFilesInPersonalFolder() {
        return fileService.getFilesInPersonalFolder()
                .switchIfEmpty(Flux.just("The directory is empty, there is nothing to delete")) // Если Flux пустой, вернуть сообщение
                .flatMap(file -> file.equals("The directory is empty, there is nothing to delete")
                        ? Flux.just(file) // Если папка пуста, вернуть сообщение
                        : fileService.deleteFilesInPersonalFolder()); // Иначе продолжить удаление
    }

    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @GetMapping("/{fileId}")
    public Mono<ResponseEntity<String>> getFileById(@PathVariable("fileId") Long fileId) {
        return fileService.getFileById(fileId)
                .flatMap(file -> {
                    if (Status.ACTIVE.equals(file.getStatus())) {
                        return Mono.just(ResponseEntity.ok(file.getLocation()));
                    } else if (Status.DELETED.equals(file.getStatus())) {
                        return Mono.just(ResponseEntity.status(HttpStatus.GONE).body("File has been deleted"));
                    } else {
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Unknown file status: " + file.getStatus()));
                    }
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found"));
    }

    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @PutMapping(value = "/{fileId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<String>> updateFile(@PathVariable("fileId") Long fileId,
                                                   @RequestPart("file") Mono<FilePart> file) {
        return deleteFile(fileId) // Сначала вызываем метод deleteFile
                .flatMap(deleteResponse -> {
                    if (deleteResponse.getStatusCode().is2xxSuccessful()) {
                        // Если файл успешно удалён, вызываем uploadFile
                        return uploadFile(file)
                                .map(uploadResponse -> ResponseEntity.ok("File successfully updated"));
                    } else {
                        // Если файл не удалён, возвращаем ошибку
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Failed to update file: could not delete existing file"));
                    }
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("An error occurred while updating the file: " + e.getMessage())));
    }
}
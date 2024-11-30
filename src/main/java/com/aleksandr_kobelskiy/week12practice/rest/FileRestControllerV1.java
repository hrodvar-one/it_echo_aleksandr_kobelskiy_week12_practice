package com.aleksandr_kobelskiy.week12practice.rest;

import com.aleksandr_kobelskiy.week12practice.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class FileRestControllerV1 {

    private final FileService fileService;

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
    public Mono<ResponseEntity<String>> uploadFile(
            @RequestPart("file") Mono<FilePart> file,
            @RequestParam("userId") Long userId) {
        return file.flatMap(filePart -> fileService.uploadAndSaveFile(filePart, userId))
                .map(location -> ResponseEntity.ok("File uploaded successfully: " + location))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage())));
    }


    @GetMapping(value = "/personal-folder", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<String> listFilesInPersonalFolder() {
        return fileService.getFilesInPersonalFolder()
                .switchIfEmpty(Flux.just("The directory is empty")); // Если Flux пустой, возвращаем сообщение
    }


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
}

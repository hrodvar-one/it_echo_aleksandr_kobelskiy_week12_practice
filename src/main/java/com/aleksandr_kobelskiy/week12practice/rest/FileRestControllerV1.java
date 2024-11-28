package com.aleksandr_kobelskiy.week12practice.rest;

import com.aleksandr_kobelskiy.week12practice.entity.FileEntity;
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

    @GetMapping("/locations")
    public Flux<ResponseEntity<String>> getAllFileLocations() {
        return fileService.getAllFileLocationsFromDB()
                .map(location -> ResponseEntity.ok(location))
                .onErrorResume(e -> Flux.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<String>> uploadFile(@RequestPart("file") Mono<FilePart> file) {
        return file.flatMap(fileService::uploadAndSaveFile)
                .map(location -> ResponseEntity.ok("File uploaded successfully: " + location))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage())));
    }

    @GetMapping
    public Flux<FileEntity> getAllFiles() {
        return fileService.getAllFiles();
    }

//    @PostMapping
//    public FileEntity create(FileEntity file) {
//        return new FileEntity();
//    }
}

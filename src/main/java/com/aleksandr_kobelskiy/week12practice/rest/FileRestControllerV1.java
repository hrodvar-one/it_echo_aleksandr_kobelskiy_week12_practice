package com.aleksandr_kobelskiy.week12practice.rest;

import com.aleksandr_kobelskiy.week12practice.entity.FileEntity;
import com.aleksandr_kobelskiy.week12practice.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class FileRestControllerV1 {

    private final FileService fileService;

    @GetMapping
    public Flux<FileEntity> getAllFiles() {
        return fileService.getAllFiles();
    }

    @PostMapping
    public FileEntity create(FileEntity file) {
        return new FileEntity();
    }
}

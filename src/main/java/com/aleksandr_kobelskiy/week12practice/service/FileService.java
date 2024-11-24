package com.aleksandr_kobelskiy.week12practice.service;

import com.aleksandr_kobelskiy.week12practice.entity.FileEntity;
import com.aleksandr_kobelskiy.week12practice.entity.Status;
import com.aleksandr_kobelskiy.week12practice.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class FileService {
    private final FileRepository fileRepository;

    public Mono<FileEntity> saveFile(String fileName, String location) {
        FileEntity file = new FileEntity();
        return fileRepository.save(
                file.toBuilder()
                        .fileName(fileName)
                        .location(location)
                        .status(Status.ACTIVE)
                        .build());
    }

    public Mono<FileEntity> getFileById(Long id) {
        return fileRepository.findById(id);
    }
}

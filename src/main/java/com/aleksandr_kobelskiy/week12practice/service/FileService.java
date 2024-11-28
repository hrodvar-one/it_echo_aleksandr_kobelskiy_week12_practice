package com.aleksandr_kobelskiy.week12practice.service;

import com.aleksandr_kobelskiy.week12practice.entity.FileEntity;
import com.aleksandr_kobelskiy.week12practice.repository.FileRepository;
import com.aleksandr_kobelskiy.week12practice.repository.s3.AwsS3Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {

    private final AwsS3Repository awsS3Repository;
    private final FileRepository fileRepository;

    public Flux<String> getAllFileLocationsFromDB() {
        return fileRepository.findAll() // Получаем все записи из базы данных
                .map(FileEntity::getLocation); // Преобразуем их в список путей (location)
    }

    public Mono<String> uploadAndSaveFile(FilePart filePart) {
        return awsS3Repository.uploadFile(filePart)
                .flatMap(location -> {
                    FileEntity fileEntity = new FileEntity();
                    fileEntity.setFileName(filePart.filename());
                    fileEntity.setLocation(location);
                    return fileRepository.save(fileEntity)
                            .thenReturn(location);
                });
    }

    public Mono<FileEntity> getFileById(Long id) {
        return fileRepository.findById(id);
    }

    public Flux<FileEntity> getAllFiles() {
        return fileRepository.findAll();
    }
}

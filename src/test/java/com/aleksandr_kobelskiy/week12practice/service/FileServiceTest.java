package com.aleksandr_kobelskiy.week12practice.service;

import com.aleksandr_kobelskiy.week12practice.entity.EventEntity;
import com.aleksandr_kobelskiy.week12practice.entity.FileEntity;
import com.aleksandr_kobelskiy.week12practice.entity.Status;
import com.aleksandr_kobelskiy.week12practice.entity.UserEntity;
import com.aleksandr_kobelskiy.week12practice.repository.EventRepository;
import com.aleksandr_kobelskiy.week12practice.repository.FileRepository;
import com.aleksandr_kobelskiy.week12practice.repository.s3.AwsS3Repository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private AwsS3Repository awsS3Repository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private EventService eventService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private FileService fileService;

    @Test
    @DisplayName("Test get all active file locations successfully")
    public void givenActiveFilesExist_whenGetAllFileLocationsFromDB_thenReturnLocations() {
        FileEntity file1 = new FileEntity(1L, "file1.txt", "location1", Status.ACTIVE);
        FileEntity file2 = new FileEntity(2L, "file2.txt", "location2", Status.ACTIVE);

        BDDMockito.given(fileRepository.findByStatus(Status.ACTIVE)).willReturn(Flux.just(file1, file2));

        Flux<String> result = fileService.getAllFileLocationsFromDB();

        StepVerifier.create(result)
                .expectNext("location1", "location2")
                .verifyComplete();

        BDDMockito.then(fileRepository).should(times(1)).findByStatus(Status.ACTIVE);
    }

    @Test
    @DisplayName("Test get all file locations when no active files exist")
    public void givenNoActiveFilesExist_whenGetAllFileLocationsFromDB_thenReturnEmptyFlux() {
        BDDMockito.given(fileRepository.findByStatus(Status.ACTIVE)).willReturn(Flux.empty());

        Flux<String> result = fileService.getAllFileLocationsFromDB();

        StepVerifier.create(result)
                .verifyComplete();

        BDDMockito.then(fileRepository).should(times(1)).findByStatus(Status.ACTIVE);
    }

    @Test
    @DisplayName("Test upload and save file successfully")
    public void givenFilePart_whenUploadAndSaveFile_thenReturnLocation() {
        FilePart filePart = mock(FilePart.class);
        BDDMockito.given(filePart.filename()).willReturn("file1.txt");

        BDDMockito.given(awsS3Repository.uploadFile(filePart)).willReturn(Mono.just("location1"));
        FileEntity savedFile = new FileEntity(1L, "file1.txt", "location1", Status.ACTIVE);
        BDDMockito.given(fileRepository.save(any(FileEntity.class))).willReturn(Mono.just(savedFile));
        BDDMockito.given(eventService.logEvent(1L, Status.ACTIVE)).willReturn(Mono.empty());

        Mono<String> result = fileService.uploadAndSaveFile(filePart);

        StepVerifier.create(result)
                .expectNext("location1")
                .verifyComplete();

        BDDMockito.then(awsS3Repository).should(times(1)).uploadFile(filePart);
        BDDMockito.then(fileRepository).should(times(1)).save(any(FileEntity.class));
        BDDMockito.then(eventService).should(times(1)).logEvent(1L, Status.ACTIVE);
    }

    @Test
    @DisplayName("Test upload and save file when upload fails")
    public void givenFilePart_whenUploadAndSaveFile_thenError() {
        FilePart filePart = mock(FilePart.class);
        BDDMockito.given(awsS3Repository.uploadFile(filePart)).willReturn(Mono.error(new RuntimeException("Upload failed")));

        Mono<String> result = fileService.uploadAndSaveFile(filePart);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        BDDMockito.then(awsS3Repository).should(times(1)).uploadFile(filePart);
        BDDMockito.then(fileRepository).should(never()).save(any(FileEntity.class));
        BDDMockito.then(eventService).should(never()).logEvent(anyLong(), any(Status.class));
    }

    @Test
    @DisplayName("Test delete file successfully")
    public void givenActiveFile_whenDeleteFile_thenFileIsDeleted() {
        Long fileId = 1L;
        FileEntity file = new FileEntity(fileId, "file1.txt", "location1", Status.ACTIVE);

        BDDMockito.given(fileRepository.findByIdAndStatus(fileId, Status.ACTIVE)).willReturn(Mono.just(file));
        BDDMockito.given(awsS3Repository.deleteFileFromS3(anyString())).willReturn(Mono.empty());
        BDDMockito.given(fileRepository.save(any(FileEntity.class))).willReturn(Mono.empty());
        BDDMockito.given(eventRepository.findByFileId(fileId)).willReturn(Flux.empty());
        BDDMockito.given(userService.getCurrentUser()).willReturn(Mono.just(UserEntity.builder()
                .id(1L)
                .username("testUser")
                .build()));
        // Добавляем мок для eventRepository.save()
        BDDMockito.given(eventRepository.save(any(EventEntity.class))).willReturn(Mono.just(new EventEntity()));

        Mono<Void> result = fileService.deleteFile(fileId);

        StepVerifier.create(result)
                .verifyComplete();

        BDDMockito.then(fileRepository).should(times(1)).findByIdAndStatus(fileId, Status.ACTIVE);
        BDDMockito.then(awsS3Repository).should(times(1)).deleteFileFromS3(anyString());
    }

    @Test
    @DisplayName("Test delete file when file does not exist")
    public void givenNonExistingFile_whenDeleteFile_thenError() {
        Long fileId = 1L;

        BDDMockito.given(fileRepository.findByIdAndStatus(fileId, Status.ACTIVE)).willReturn(Mono.empty());

        Mono<Void> result = fileService.deleteFile(fileId);

        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();

        BDDMockito.then(fileRepository).should(times(1)).findByIdAndStatus(fileId, Status.ACTIVE);
        BDDMockito.then(awsS3Repository).should(never()).deleteFileFromS3(anyString());
    }

    @Test
    @DisplayName("Test get files in personal folder successfully")
    public void whenGetFilesInPersonalFolder_thenReturnFileNames() {
        BDDMockito.given(awsS3Repository.listFilesInPersonalFolder()).willReturn(Flux.just("file1.txt", "file2.txt"));

        Flux<String> result = fileService.getFilesInPersonalFolder();

        StepVerifier.create(result)
                .expectNext("file1.txt", "file2.txt")
                .verifyComplete();

        BDDMockito.then(awsS3Repository).should(times(1)).listFilesInPersonalFolder();
    }

    @Test
    @DisplayName("Test get files in personal folder when no files exist")
    public void whenGetFilesInPersonalFolder_thenReturnEmptyFlux() {
        BDDMockito.given(awsS3Repository.listFilesInPersonalFolder()).willReturn(Flux.empty());

        Flux<String> result = fileService.getFilesInPersonalFolder();

        StepVerifier.create(result)
                .verifyComplete();

        BDDMockito.then(awsS3Repository).should(times(1)).listFilesInPersonalFolder();
    }

    @Test
    @DisplayName("Test delete files in personal folder successfully")
    public void whenDeleteFilesInPersonalFolder_thenFilesAreDeleted() {
        BDDMockito.given(awsS3Repository.deleteAllFilesInPersonalFolder()).willReturn(Flux.just("file1.txt", "file2.txt"));

        Flux<String> result = fileService.deleteFilesInPersonalFolder();

        StepVerifier.create(result)
                .expectNext("file1.txt", "file2.txt")
                .verifyComplete();

        BDDMockito.then(awsS3Repository).should(times(1)).deleteAllFilesInPersonalFolder();
    }

    @Test
    @DisplayName("Test delete files in personal folder when no files exist")
    public void whenDeleteFilesInPersonalFolder_thenReturnEmptyFlux() {
        BDDMockito.given(awsS3Repository.deleteAllFilesInPersonalFolder()).willReturn(Flux.empty());

        Flux<String> result = fileService.deleteFilesInPersonalFolder();

        StepVerifier.create(result)
                .verifyComplete();

        BDDMockito.then(awsS3Repository).should(times(1)).deleteAllFilesInPersonalFolder();
    }

    @Test
    @DisplayName("Test get file by ID successfully")
    public void givenExistingFileId_whenGetFileById_thenReturnFileEntity() {
        Long fileId = 1L;
        FileEntity file = new FileEntity(fileId, "file1.txt", "location1", Status.ACTIVE);

        BDDMockito.given(fileRepository.findById(fileId)).willReturn(Mono.just(file));

        Mono<FileEntity> result = fileService.getFileById(fileId);

        StepVerifier.create(result)
                .expectNext(file)
                .verifyComplete();

        BDDMockito.then(fileRepository).should(times(1)).findById(fileId);
    }

    @Test
    @DisplayName("Test get file by ID when file does not exist")
    public void givenNonExistingFileId_whenGetFileById_thenReturnEmptyMono() {
        Long fileId = 1L;

        BDDMockito.given(fileRepository.findById(fileId)).willReturn(Mono.empty());

        Mono<FileEntity> result = fileService.getFileById(fileId);

        StepVerifier.create(result)
                .verifyComplete();

        BDDMockito.then(fileRepository).should(times(1)).findById(fileId);
    }
}
package com.dfs.file;

import com.dfs.common.exception.ApiException;
import com.dfs.file.dto.FileResponse;
import com.dfs.user.User;
import com.dfs.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileServiceTest {

    private final FileMetadataRepository fileRepository = mock(FileMetadataRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final StorageService storageService = mock(StorageService.class);
    private final FileService fileService =
            new FileService(fileRepository, userRepository, storageService);

    private User owner;

    @BeforeEach
    void setup() {
        owner = new User("naresh@example.com", "HASHED");
        // give the mock user an id via reflection-free helper: stub findByEmail
        when(userRepository.findByEmail("naresh@example.com")).thenReturn(Optional.of(owner));
    }

    @Test
    void uploadStoresBytesAndSavesMetadata() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "hello.txt", "text/plain", "hello world".getBytes());
        when(fileRepository.save(any(FileMetadata.class))).thenAnswer(inv -> inv.getArgument(0));

        FileResponse response = fileService.upload("naresh@example.com", file);

        assertThat(response.filename()).isEqualTo("hello.txt");
        assertThat(response.size()).isEqualTo("hello world".getBytes().length);
        assertThat(response.contentType()).isEqualTo("text/plain");
        assertThat(response.checksum()).hasSize(64); // SHA-256 hex
        verify(storageService).store(anyString(), any(InputStream.class), anyLong(), eq("text/plain"));
        verify(fileRepository).save(any(FileMetadata.class));
    }

    @Test
    void uploadRejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> fileService.upload("naresh@example.com", empty))
                .isInstanceOf(ApiException.class);

        verify(storageService, never()).store(anyString(), any(), anyLong(), anyString());
        verify(fileRepository, never()).save(any());
    }

    @Test
    void listReturnsOnlyOwnerFiles() {
        FileMetadata f = new FileMetadata(owner, "a.txt", 3, "text/plain", "abc", "key1");
        when(fileRepository.findAllByOwnerId(any())).thenReturn(List.of(f));

        List<FileResponse> files = fileService.listForOwner("naresh@example.com");

        assertThat(files).hasSize(1);
        assertThat(files.get(0).filename()).isEqualTo("a.txt");
    }

    @Test
    void getOwnedFileThrowsWhenNotFound() {
        when(fileRepository.findByIdAndOwnerId(anyLong(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.getOwnedFile("naresh@example.com", 99L))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    void deleteRemovesBytesAndMetadata() {
        FileMetadata f = new FileMetadata(owner, "a.txt", 3, "text/plain", "abc", "key1");
        when(fileRepository.findByIdAndOwnerId(eq(1L), any())).thenReturn(Optional.of(f));

        fileService.delete("naresh@example.com", 1L);

        verify(storageService).delete("key1");
        verify(fileRepository).delete(f);
    }

    @Test
    void downloadRetrievesFromStorage() {
        FileMetadata f = new FileMetadata(owner, "a.txt", 3, "text/plain", "abc", "key1");
        InputStream fake = InputStream.nullInputStream();
        when(fileRepository.findByIdAndOwnerId(eq(1L), any())).thenReturn(Optional.of(f));
        when(storageService.retrieve("key1")).thenReturn(fake);

        InputStream result = fileService.download("naresh@example.com", 1L);

        assertThat(result).isSameAs(fake);
        verify(storageService).retrieve("key1");
    }
}

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
    private final StoredObjectRepository storedObjectRepository = mock(StoredObjectRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final StorageService storageService = mock(StorageService.class);
    private final FileService fileService =
            new FileService(fileRepository, storedObjectRepository, userRepository, storageService);

    private User owner;

    @BeforeEach
    void setup() {
        owner = new User("naresh@example.com", "HASHED");
        when(userRepository.findByEmail("naresh@example.com")).thenReturn(Optional.of(owner));
        when(fileRepository.save(any(FileMetadata.class))).thenAnswer(inv -> inv.getArgument(0));
        when(storedObjectRepository.save(any(StoredObject.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void uploadNewContentStoresBytesAndCreatesStoredObject() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "hello.txt", "text/plain", "hello world".getBytes());
        // no existing content with this checksum
        when(storedObjectRepository.findByChecksum(anyString())).thenReturn(Optional.empty());

        FileResponse response = fileService.upload("naresh@example.com", file);

        assertThat(response.filename()).isEqualTo("hello.txt");
        assertThat(response.checksum()).hasSize(64);
        // new content: bytes ARE stored, a StoredObject IS created
        verify(storageService).store(anyString(), any(InputStream.class), anyLong(), eq("text/plain"));
        verify(storedObjectRepository).save(any(StoredObject.class));
    }

    @Test
    void uploadDuplicateContentSkipsStorageAndIncrementsReferenceCount() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "hello.txt", "text/plain", "hello world".getBytes());
        // identical content already exists
        StoredObject existing = new StoredObject("dummychecksum", "existing-key", 11);
        when(storedObjectRepository.findByChecksum(anyString())).thenReturn(Optional.of(existing));

        int before = existing.getReferenceCount();
        fileService.upload("naresh@example.com", file);

        // duplicate: bytes are NOT stored again, reference count incremented
        verify(storageService, never()).store(anyString(), any(), anyLong(), anyString());
        assertThat(existing.getReferenceCount()).isEqualTo(before + 1);
    }

    @Test
    void uploadRejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> fileService.upload("naresh@example.com", empty))
                .isInstanceOf(ApiException.class);

        verify(storageService, never()).store(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void listReturnsOnlyOwnerFiles() {
        StoredObject so = new StoredObject("abc", "key1", 3);
        FileMetadata f = new FileMetadata(owner, so, "a.txt", 3, "text/plain");
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
    void deleteWithMultipleReferencesKeepsBytes() {
        StoredObject so = new StoredObject("abc", "key1", 3);
        so.incrementReferenceCount(); // now 2 references
        FileMetadata f = new FileMetadata(owner, so, "a.txt", 3, "text/plain");
        when(fileRepository.findByIdAndOwnerId(eq(1L), any())).thenReturn(Optional.of(f));

        fileService.delete("naresh@example.com", 1L);

        // still referenced by another file: bytes are KEPT, count decremented
        verify(storageService, never()).delete(anyString());
        verify(storedObjectRepository, never()).delete(any());
        assertThat(so.getReferenceCount()).isEqualTo(1);
    }

    @Test
    void deleteLastReferenceRemovesBytes() {
        StoredObject so = new StoredObject("abc", "key1", 3); // 1 reference
        FileMetadata f = new FileMetadata(owner, so, "a.txt", 3, "text/plain");
        when(fileRepository.findByIdAndOwnerId(eq(1L), any())).thenReturn(Optional.of(f));

        fileService.delete("naresh@example.com", 1L);

        // last reference gone: bytes AND stored object are deleted
        verify(storageService).delete("key1");
        verify(storedObjectRepository).delete(so);
    }

    @Test
    void downloadRetrievesFromStorage() {
        StoredObject so = new StoredObject("abc", "key1", 3);
        FileMetadata f = new FileMetadata(owner, so, "a.txt", 3, "text/plain");
        InputStream fake = InputStream.nullInputStream();
        when(fileRepository.findByIdAndOwnerId(eq(1L), any())).thenReturn(Optional.of(f));
        when(storageService.retrieve("key1")).thenReturn(fake);

        InputStream result = fileService.download("naresh@example.com", 1L);

        assertThat(result).isSameAs(fake);
        verify(storageService).retrieve("key1");
    }
}

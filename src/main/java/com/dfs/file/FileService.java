package com.dfs.file;

import com.dfs.common.exception.ApiException;
import com.dfs.file.dto.FileResponse;
import com.dfs.user.User;
import com.dfs.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates file operations: checksum, store/retrieve/delete bytes in MinIO
 * via {@link StorageService}, and persist metadata in PostgreSQL. Every file is
 * owned by the user resolved from the JWT; users can only touch their own files.
 */
@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final FileMetadataRepository fileRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    public FileService(FileMetadataRepository fileRepository,
                       UserRepository userRepository,
                       StorageService storageService) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    @Transactional
    public FileResponse upload(String ownerEmail, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File must not be empty");
        }

        User owner = resolveUser(ownerEmail);
        String checksum = sha256(file);
        String objectKey = owner.getId() + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

        try (InputStream in = file.getInputStream()) {
            storageService.store(objectKey, in, file.getSize(),
                    contentTypeOf(file));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read upload: " + e.getMessage());
        }

        FileMetadata saved = fileRepository.save(new FileMetadata(
                owner, file.getOriginalFilename(), file.getSize(),
                contentTypeOf(file), checksum, objectKey));

        log.info("Stored file id={} name={} size={} owner={}",
                saved.getId(), saved.getFilename(), saved.getSize(), owner.getEmail());
        return FileResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<FileResponse> listForOwner(String ownerEmail) {
        User owner = resolveUser(ownerEmail);
        return fileRepository.findAllByOwnerId(owner.getId())
                .stream().map(FileResponse::from).toList();
    }

    /** Returns the metadata (with owner check) for a download. */
    @Transactional(readOnly = true)
    public FileMetadata getOwnedFile(String ownerEmail, Long fileId) {
        User owner = resolveUser(ownerEmail);
        return fileRepository.findByIdAndOwnerId(fileId, owner.getId())
                .orElseThrow(() -> new FileNotFoundException(fileId));
    }

    /** Streams the bytes for a file the user owns. */
    @Transactional(readOnly = true)
    public InputStream download(String ownerEmail, Long fileId) {
        FileMetadata meta = getOwnedFile(ownerEmail, fileId);
        return storageService.retrieve(meta.getObjectKey());
    }

    @Transactional
    public void delete(String ownerEmail, Long fileId) {
        FileMetadata meta = getOwnedFile(ownerEmail, fileId);
        storageService.delete(meta.getObjectKey());
        fileRepository.delete(meta);
        log.info("Deleted file id={} owner={}", fileId, ownerEmail);
    }

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Unknown user"));
    }

    private String contentTypeOf(MultipartFile file) {
        return file.getContentType() != null ? file.getContentType() : "application/octet-stream";
    }

    private String sha256(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to checksum file: " + e.getMessage());
        }
    }
}

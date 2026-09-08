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
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates file operations with content deduplication.
 *
 * <p>On upload, the file's SHA-256 checksum is looked up: if identical content
 * already exists (a {@link StoredObject}), the new file references it and the
 * reference count is incremented — the bytes are NOT stored again. On delete,
 * the reference count is decremented, and the bytes are removed only when the
 * count reaches zero (no file references them anymore).
 */
@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final FileMetadataRepository fileRepository;
    private final StoredObjectRepository storedObjectRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    public FileService(FileMetadataRepository fileRepository,
                       StoredObjectRepository storedObjectRepository,
                       UserRepository userRepository,
                       StorageService storageService) {
        this.fileRepository = fileRepository;
        this.storedObjectRepository = storedObjectRepository;
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

        // Deduplication: does identical content already exist?
        Optional<StoredObject> existing = storedObjectRepository.findByChecksum(checksum);

        StoredObject storedObject;
        if (existing.isPresent()) {
            // Content already stored — reference it, skip the byte upload.
            storedObject = existing.get();
            storedObject.incrementReferenceCount();
            storedObjectRepository.save(storedObject);
            log.info("Deduplicated upload: checksum={} now referenced {} time(s)",
                    checksum, storedObject.getReferenceCount());
        } else {
            // New content — upload the bytes and create a stored object.
            String objectKey = owner.getId() + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
            try (InputStream in = file.getInputStream()) {
                storageService.store(objectKey, in, file.getSize(), contentTypeOf(file));
            } catch (IOException e) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to read upload: " + e.getMessage());
            }
            storedObject = storedObjectRepository.save(
                    new StoredObject(checksum, objectKey, file.getSize()));
            log.info("Stored new content: checksum={} objectKey={}", checksum, objectKey);
        }

        FileMetadata saved = fileRepository.save(new FileMetadata(
                owner, storedObject, file.getOriginalFilename(),
                file.getSize(), contentTypeOf(file)));

        log.info("File record created id={} name={} owner={}",
                saved.getId(), saved.getFilename(), owner.getEmail());
        return FileResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<FileResponse> listForOwner(String ownerEmail) {
        User owner = resolveUser(ownerEmail);
        return fileRepository.findAllByOwnerId(owner.getId())
                .stream().map(FileResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public FileMetadata getOwnedFile(String ownerEmail, Long fileId) {
        User owner = resolveUser(ownerEmail);
        return fileRepository.findByIdAndOwnerId(fileId, owner.getId())
                .orElseThrow(() -> new FileNotFoundException(fileId));
    }

    @Transactional(readOnly = true)
    public InputStream download(String ownerEmail, Long fileId) {
        FileMetadata meta = getOwnedFile(ownerEmail, fileId);
        return storageService.retrieve(meta.getStoredObject().getObjectKey());
    }

    @Transactional
    public void delete(String ownerEmail, Long fileId) {
        FileMetadata meta = getOwnedFile(ownerEmail, fileId);
        StoredObject storedObject = meta.getStoredObject();

        // Remove the file record first.
        fileRepository.delete(meta);

        // Reference counting: only delete the bytes when nothing references them.
        storedObject.decrementReferenceCount();
        if (storedObject.getReferenceCount() <= 0) {
            storageService.delete(storedObject.getObjectKey());
            storedObjectRepository.delete(storedObject);
            log.info("Deleted content: checksum={} (no more references)",
                    storedObject.getChecksum());
        } else {
            storedObjectRepository.save(storedObject);
            log.info("Kept content: checksum={} still referenced {} time(s)",
                    storedObject.getChecksum(), storedObject.getReferenceCount());
        }

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
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to checksum file: " + e.getMessage());
        }
    }
}

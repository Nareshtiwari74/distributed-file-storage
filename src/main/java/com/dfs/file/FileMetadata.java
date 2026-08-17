package com.dfs.file;

import com.dfs.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Metadata about an uploaded file. The actual bytes live in MinIO,
 * referenced by {@link #objectKey}. This entity is never returned
 * directly over the API — controllers use DTOs.
 */
@Entity
@Table(name = "files")
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private long size;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(name = "object_key", nullable = false, unique = true)
    private String objectKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FileMetadata() {
    }

    public FileMetadata(User owner, String filename, long size,
                        String contentType, String checksum, String objectKey) {
        this.owner = owner;
        this.filename = filename;
        this.size = size;
        this.contentType = contentType;
        this.checksum = checksum;
        this.objectKey = objectKey;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getOwner() { return owner; }
    public String getFilename() { return filename; }
    public long getSize() { return size; }
    public String getContentType() { return contentType; }
    public String getChecksum() { return checksum; }
    public String getObjectKey() { return objectKey; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

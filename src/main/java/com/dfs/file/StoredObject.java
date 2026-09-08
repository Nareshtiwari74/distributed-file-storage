package com.dfs.file;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A unique piece of stored content, identified by its SHA-256 checksum.
 * Many FileMetadata records may reference the same StoredObject (dedup).
 * referenceCount tracks how many files point to it; at zero, the bytes
 * can be deleted.
 */
@Entity
@Table(name = "stored_objects")
public class StoredObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String checksum;

    @Column(name = "object_key", nullable = false, unique = true)
    private String objectKey;

    @Column(nullable = false)
    private long size;

    @Column(name = "reference_count", nullable = false)
    private int referenceCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StoredObject() {
    }

    public StoredObject(String checksum, String objectKey, long size) {
        this.checksum = checksum;
        this.objectKey = objectKey;
        this.size = size;
        this.referenceCount = 1;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void incrementReferenceCount() {
        this.referenceCount++;
    }

    public void decrementReferenceCount() {
        this.referenceCount--;
    }

    public Long getId() { return id; }
    public String getChecksum() { return checksum; }
    public String getObjectKey() { return objectKey; }
    public long getSize() { return size; }
    public int getReferenceCount() { return referenceCount; }
    public Instant getCreatedAt() { return createdAt; }
}

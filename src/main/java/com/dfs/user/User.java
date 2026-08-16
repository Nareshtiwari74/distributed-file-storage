
package com.dfs.user;

import jakarta.persistence.Column;

import jakarta.persistence.Entity;

import jakarta.persistence.GeneratedValue;

import jakarta.persistence.GenerationType;

import jakarta.persistence.Id;

import jakarta.persistence.PrePersist;

import jakarta.persistence.PreUpdate;

import jakarta.persistence.Table;

import java.time.Instant;

/**

 * JPA entity mapped to the {@code users} table (created by Flyway V1).

 * Never exposed directly over the API — controllers use DTOs.

 */

@Entity

@Table(name = "users")

public class User {

    @Id

    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;

    @Column(nullable = false, unique = true)

    private String email;

    @Column(nullable = false)

    private String password;

    @Column(name = "created_at", nullable = false, updatable = false)

    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)

    private Instant updatedAt;

    protected User() {

        // required by JPA

    }

    public User(String email, String password) {

        this.email = email;

        this.password = password;

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

    public Long getId() {

        return id;

    }

    public String getEmail() {

        return email;

    }

    public String getPassword() {

        return password;

    }

    public void setPassword(String password) {

        this.password = password;

    }

    public Instant getCreatedAt() {

        return createdAt;

    }

    public Instant getUpdatedAt() {

        return updatedAt;

    }

}


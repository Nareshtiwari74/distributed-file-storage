package com.dfs.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full file lifecycle against real PostgreSQL + real MinIO (Testcontainers).
 * Tagged "integration": runs only where Testcontainers can reach Docker
 * (e.g. CI). Skipped by the default build.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class FileIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static GenericContainer<?> minio = new GenericContainer<>(
            DockerImageName.parse("minio/minio:latest"))
            .withCommand("server /data")
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withExposedPorts(9000);

    @org.springframework.test.context.DynamicPropertySource
    static void minioProps(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("minio.endpoint",
                () -> "http://" + minio.getHost() + ":" + minio.getMappedPort(9000));
        registry.add("minio.access-key", () -> "minioadmin");
        registry.add("minio.secret-key", () -> "minioadmin");
        registry.add("minio.bucket", () -> "dfs-files-test");
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String registerAndGetToken() throws Exception {
        String body = """
                {"email":"file-it@example.com","password":"password123"}
                """;
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    @Test
    void fullFileLifecycle() throws Exception {
        String token = registerAndGetToken();
        String auth = "Bearer " + token;
        byte[] content = "distributed storage test content".getBytes();

        MockMultipartFile upload = new MockMultipartFile(
                "file", "test.txt", "text/plain", content);

        // Upload -> 201
        MvcResult uploadResult = mockMvc.perform(multipart("/api/files")
                        .file(upload).header("Authorization", auth))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("test.txt"))
                .andExpect(jsonPath("$.checksum").isNotEmpty())
                .andReturn();

        long id = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .get("id").asLong();

        // List -> contains the file
        mockMvc.perform(get("/api/files").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filename").value("test.txt"));

        // Download -> exact bytes back
        mockMvc.perform(get("/api/files/" + id + "/download").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(content().bytes(content));

        // Delete -> 204
        mockMvc.perform(delete("/api/files/" + id).header("Authorization", auth))
                .andExpect(status().isNoContent());

        // List -> empty
        mockMvc.perform(get("/api/files").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void cannotAccessWithoutToken() throws Exception {
        mockMvc.perform(get("/api/files"))
                .andExpect(status().isUnauthorized());
    }
}

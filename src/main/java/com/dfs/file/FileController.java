package com.dfs.file;

import com.dfs.file.dto.FileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * File endpoints. All require authentication (see SecurityConfig). The current
 * user's email comes from the Authentication set by JwtAuthenticationFilter.
 */
@Tag(name = "Files", description = "Upload, list, download, delete files")
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @Operation(summary = "Upload a file")
    @PostMapping
    public ResponseEntity<FileResponse> upload(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        FileResponse response = fileService.upload(authentication.getName(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List my files")
    @GetMapping
    public ResponseEntity<List<FileResponse>> list(Authentication authentication) {
        return ResponseEntity.ok(fileService.listForOwner(authentication.getName()));
    }

    @Operation(summary = "Download a file")
    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable Long id,
            Authentication authentication) {
        FileMetadata meta = fileService.getOwnedFile(authentication.getName(), id);
        InputStream stream = fileService.download(authentication.getName(), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + meta.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(meta.getContentType()))
                .contentLength(meta.getSize())
                .body(new InputStreamResource(stream));
    }

    @Operation(summary = "Delete a file")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication authentication) {
        fileService.delete(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}

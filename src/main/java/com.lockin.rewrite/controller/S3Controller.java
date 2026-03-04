package com.lockin.rewrite.controller;

import com.lockin.rewrite.service.S3Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000") // Allow frontend to call
public class S3Controller {

    private final S3Service s3Service;

    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @PostMapping("/presigned-upload")
    public ResponseEntity<?> getPresignedUploadUrl(@RequestBody Map<String, String> payload) {
        String fileName = payload.get("fileName");
        String fileType = payload.get("fileType");

        if (fileName == null || fileType == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing fileName or fileType"));
        }

        boolean isResume = fileName.toLowerCase().contains("resume") ||
                fileType.contains("wordprocessingml.document");
        String prefix = isResume ? "resumes" : "job-descriptions";

        // Generate a unique key
        String fileKey = s3Service.generateUniqueFileKey(fileName, prefix);

        String uploadUrl = s3Service.generatePresignedUploadUrl(fileKey, fileType, 300); // 5 mins

        return ResponseEntity.ok(Map.of(
                "url", uploadUrl,
                "key", fileKey,
                "expiresIn", 300));
    }

    @PostMapping("/download")
    public ResponseEntity<?> getDownloadUrl(@RequestBody Map<String, String> payload) {
        String fileKey = payload.get("fileKey");

        if (fileKey == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "fileKey is required"));
        }

        String downloadUrl = s3Service.generatePresignedDownloadUrl(fileKey, 3600); // 1 hour

        return ResponseEntity.ok(Map.of("downloadUrl", downloadUrl));
    }
}

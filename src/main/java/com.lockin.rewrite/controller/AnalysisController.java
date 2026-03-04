package com.lockin.rewrite.controller;

import com.lockin.rewrite.model.AnalysisResponse;
import com.lockin.rewrite.model.ResumeData;
import com.lockin.rewrite.service.DocumentParserService;
import com.lockin.rewrite.service.LatexService;
import com.lockin.rewrite.service.ResumeAnalyzerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class AnalysisController {

    private final S3Client s3Client;
    private final DocumentParserService documentParserService;
    private final ResumeAnalyzerService resumeAnalyzerService;
    private final String bucketName;
    private final LatexService latexService;

    public AnalysisController(S3Client s3Client,
                              DocumentParserService documentParserService,
                              ResumeAnalyzerService resumeAnalyzerService,
                              LatexService latexService,
                              @Value("${aws.s3.bucketName}") String bucketName) {
        this.s3Client = s3Client;
        this.documentParserService = documentParserService;
        this.resumeAnalyzerService = resumeAnalyzerService;
        this.latexService = latexService;
        this.bucketName = bucketName;
    }

    @PostMapping("/process")
    public ResponseEntity<?> processResume(@RequestBody Map<String, String> payload) {
        try {
            String resumeKey = payload.get("resumeKey");
            String jobDescription = payload.get("jobDescription");

            if (resumeKey == null || jobDescription == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing Resume and/or jobDescription"));
            }

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(resumeKey)
                    .build());

            byte[] fileData = objectBytes.asByteArray();

            String resumeText;
            if (resumeKey.toLowerCase().endsWith(".pdf")) {
                resumeText = documentParserService.parsePdf(fileData);
            } else {
                resumeText = documentParserService.parseDocx(fileData);
            }

            List<String> missingKeywords = new ArrayList<>(); // TODO: Implement matching logic if strictly needed

            AnalysisResponse result = resumeAnalyzerService.analyzeResume(resumeText, jobDescription, missingKeywords, resumeKey);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/generate-pdf")
    public ResponseEntity<?> generatePdf(@RequestBody ResumeData resumeData) {
        try {
            byte[] pdfBytes = latexService.generatePdf(resumeData);
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"resume.pdf\"")
                    .body(pdfBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}

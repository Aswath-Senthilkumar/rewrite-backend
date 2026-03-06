package com.lockin.rewrite.controller;

import com.lockin.rewrite.model.AnalysisResponse;
import com.lockin.rewrite.model.ResumeData;
import com.lockin.rewrite.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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
    private final DocumentAnalyzerService documentAnalyzerService;
    private final KeywordScannerService keywordScannerService;

    public AnalysisController(S3Client s3Client,
                              DocumentParserService documentParserService,
                              ResumeAnalyzerService resumeAnalyzerService,
                              LatexService latexService,
                              @Value("${aws.s3.bucketName}") String bucketName,
                              DocumentAnalyzerService documentAnalyzerService,
                              KeywordScannerService keywordScannerService) {
        this.s3Client = s3Client;
        this.documentParserService = documentParserService;
        this.resumeAnalyzerService = resumeAnalyzerService;
        this.latexService = latexService;
        this.bucketName = bucketName;
        this.documentAnalyzerService = documentAnalyzerService;
        this.keywordScannerService = keywordScannerService;
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

            // Use KeywordScannerService to find missing keywords
            KeywordScannerService.KeywordAnalysisResult keywordAnalysis = keywordScannerService.analyze(jobDescription, resumeText);
            List<String> missingKeywords = keywordAnalysis.getMissingFromResume();

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

    @PostMapping("/get-resume")
    public ResponseEntity<?> getResume(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            System.out.println("Content-Type: " + request.getContentType());
            
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            byte[] fileData = file.getBytes();
            String resumeText = documentParserService.parseDocx(fileData);

            ResumeData resumeData = documentAnalyzerService.analyzeDocument(resumeText);
            byte[] resumeBytes = latexService.generatePdf(resumeData);

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"resume.pdf\"")
                    .body(resumeBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}

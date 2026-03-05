package com.lockin.rewrite.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.lockin.rewrite.model.AnalysisResponse;
import com.lockin.rewrite.model.ResumeData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class DocumentAnalyzerService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DocumentAnalyzerService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public ResumeData analyzeDocument(String resumeText) {
        String prompt = buildPrompt(resumeText);
        try {
            String jsonResponse = callGeminiApi(prompt);
            return parseResponse(jsonResponse, resumeText);
        } catch (Exception e) {
            System.err.println("Error in analyzeDocument: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private String buildPrompt(String resumeText) {
        String truncatedResume = resumeText.length() > 10000 ? resumeText.substring(0, 10000) : resumeText;

        return String.format(
                """
                    You are an expert Resume Parser. Your task is to extract information from the resume text and structure it into a JSON format.
                    
                    **Core Logic**:
                    1. **Structure Extraction**: EXTRACT the entire resume content into a structured format.
                    2. **Sanitization**: Ensure the extracted text is clean and free of artifacts.
                    
                    **Constraints**:
                    - **CRITICAL**: DO NOT REMOVE INFORMATION. Preserve all original details, numbers, and context.
                    - **Experience & Project Summaries**:
                      * **EXTRACT** the summary exactly as it appears in the resume text.
                      * **DO NOT** summarize, rewrite, or shorten it.
                      * If no summary exists, return an empty string. **DO NOT** generate a summary.
                    - **Project Location**: Extract if present in input.
                    - **Work Experience Summary**: If 'description' text exists in input that isn't a bullet point, treat it as summary.
                    - **Formatting**:
                      * **NO MARKDOWN**: The text must be plain text. Do NOT use **bold** or *italics*.
                    
                    **Resume Text**:
                    %s
                    
                    **OUTPUT FORMAT**:
                    Return ONLY a raw JSON object (no markdown). Use this exact structure:
                    {
                      "resumeData": {
                        "personalInfo": {
                          "name": "string",
                          "phone": "string",
                          "email": "string",
                          "linkedin": "url or empty",
                          "portfolio": "url or empty"
                        },
                        "summary": "string (Professional Summary)",
                        "education": [
                          { "institution": "string", "date": "string", "degree": "string", "gpa": "string" }
                        ],
                        "skills": {
                          "languages": "string",
                          "frameworks": "string",
                          "tools": "string"
                        },
                        "experience": [
                          {
                            "title": "string",
                            "company": "string",
                            "date": "string",
                            "location": "string",
                            "summary": "string (Max 6 words, or empty)",
                            "bulletPoints": [
                              { "original": "original text", "improved": "improved text", "accepted": false }
                            ]
                          }
                        ],
                        "projects": [
                          {
                            "title": "string",
                            "link": "url/string",
                            "date": "string",
                            "summary": "string (Max 6 words, or empty)",
                            "location": "string (or empty)",
                            "bulletPoints": [
                              { "original": "original text", "improved": "improved text", "accepted": false }
                            ]
                          }
                        ],
                        "certifications": [
                          { "name": "string", "issuer": "string", "date": "string" }
                        ],
                        "awards": [
                          { "title": "string", "issuer": "string", "date": "string" }
                        ]
                      }
                    }
                    """,
                truncatedResume);
    }

    private String callGeminiApi(String prompt) {
        String urlWithKey = apiUrl + "?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> requestBody = Map.of("contents", List.of(content));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        int maxRetries = 3;
        int retryDelay = 2000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(urlWithKey, entity, String.class);
                return extractContentFromResponse(response.getBody());
            } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
                System.err.println("Gemini 429 Rate Limit hit. Attempt " + attempt + " of " + maxRetries);
                if (attempt == maxRetries) {
                    throw new RuntimeException("Gemini API Rate Limit Exceeded after retries: " + e.getMessage(), e);
                }
                try {
                    Thread.sleep(retryDelay * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry wait", ie);
                }
            } catch (Exception e) {
                System.err.println("Gemini API Call Failed. URL: " + urlWithKey);
                System.err.println("Request Body: " + requestBody);
                throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
            }
        }
        throw new RuntimeException("Unreachable code in callGeminiApi");
    }

    private String extractContentFromResponse(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            return root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        } catch (Exception e) {
            System.err.println("Failed to parse Gemini API response. Raw Response: " + rawJson);
            throw new RuntimeException("Failed to parse Gemini API response", e);
        }
    }

    private ResumeData parseResponse(String llmOutput, String resumeText) {
        try {
            String cleanJson = extractJsonBlock(llmOutput);
            // We use AnalysisResponse as a wrapper to match the JSON structure { "resumeData": ... }
            AnalysisResponse wrapper = objectMapper.readValue(cleanJson, AnalysisResponse.class);
            
            ResumeData data = wrapper.getResumeData();
            if (data == null) {
                data = new ResumeData();
            }
            
            sanitizeResponse(data);

            return data;
        } catch (Exception e) {
            System.err.println("LLM Output that failed parsing: " + llmOutput);
            throw new RuntimeException("Failed to parse LLM JSON output", e);
        }
    }

    private void sanitizeResponse(ResumeData data) {
        if (data == null)
            return;

        if (data.getExperience() != null) {
            data.getExperience().forEach(exp -> {
                if (exp.getBulletPoints() != null) {
                    exp.getBulletPoints().forEach(bp -> {
                        if (bp.getImproved() != null) {
                            bp.setImproved(bp.getImproved().replace("**", "").replace("*", ""));
                        }
                    });
                }
            });
        }

        if (data.getProjects() != null) {
            data.getProjects().forEach(proj -> {
                if (proj.getBulletPoints() != null) {
                    proj.getBulletPoints().forEach(bp -> {
                        if (bp.getImproved() != null) {
                            bp.setImproved(bp.getImproved().replace("**", "").replace("*", ""));
                        }
                    });
                }
            });
        }
    }

    private String extractJsonBlock(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        }
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }

        int start = trimmed.indexOf("{");
        int end = trimmed.lastIndexOf("}");

        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }
}

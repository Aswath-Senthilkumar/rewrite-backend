package com.lockin.rewrite.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.lockin.rewrite.model.AnalysisResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.color.ICC_ColorSpace;
import java.util.List;
import java.util.Map;

@Service
public class ResumeAnalyzerService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ResumeAnalyzerService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();

        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @org.springframework.cache.annotation.Cacheable(value = "analyses", key = "{#resumeKey, #jobDescription")
    public AnalysisResponse analyzeResume(String resumeText, String jobDescription, List<String> missingKeywordsIgnored, String resumeKey) {
        String prompt = buildPrompt(resumeText, jobDescription);
        try {
            String jsonResponse = callGeminiApi(prompt);
            return parseResponse(jsonResponse, resumeText);
        } catch (Exception e) {
            System.err.println("Error in analyzeResume" + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private String buildPrompt(String resumeText, String jobDescription) {
        String truncatedResume = resumeText.length() > 10000 ? resumeText.substring(0, 10000) : resumeText;
        String truncatedJD = jobDescription.length() > 5000 ? jobDescription.substring(0, 5000) : jobDescription;

        return String.format(
                """
                    You are an expert Talent Acquisition Specialist and Career Coach. Validate the resume against the Job Description (JD).
        
                    **Core Logic**:
                    1. **Match Score**: 0-100. Evaluate based on specific Hard Skills, Soft Skills, Tools, and Cultural Fit.
                    2. **Keywords Extraction**:
                       - **jdKeywords**: Extract all critical technical and soft skills from the Job Description (e.g. "Java", "Agile").
                       - **matchKeywords**: Extract the subset of 'jdKeywords' that are explicitly present in the Resume.
                       - **matchKeywords**: Extract the subset of 'jdKeywords' that are explicitly present in the Resume.
                       - **missingKeywords**: Identify critical requirements (Tech Stack, Methodologies) in 'jdKeywords' that are COMPLETELY ABSENT from the Resume. **CRITICAL**: If a word appears *anywhere* in the resume text (even in a different section), you MUST NOT list it as missing.
                       - **addedKeywords**: Identify keywords that you have ADDED to the resume content during the improvement/rewrite process to better align with the JD.
                    3. **Structure Extraction**: EXTRACT the entire resume content into a structured format.
                    4. **Improvements**: REWRITE bullet points to be impactful, result-oriented, and aligned with the JD's tone.
                       - **STRATEGY**: Look at the 'missingKeywords' list. Try to **INTELLIGENTLY WEAVE** these missing keywords into the 'Improved' bullet points where they fit contextually.
                       - **GOAL**: The 'Improved' version should effectively "fill the gaps" and increase the match score.
                       - **CONSTRAINT**: Do not force a keyword if it makes no sense. The new text must remain truthful to the original experience, just framed better to highlight the skill if applicable.
        
                    **Constraints**:
                    - **CRITICAL**: DO NOT REMOVE INFORMATION. Preserve all original details, numbers, and context.
                    - **Tone**: Professional, confident, and action-oriented.
                    - **Experience & Project Summaries**:
                      * **EXTRACT** the summary exactly as it appears in the resume text.
                      * **DO NOT** summarize, rewrite, or shorten it.
                      * If no summary exists, return an empty string. **DO NOT** generate a summary.
                    - **Project Location**: Extract if present in input.
                    - **Work Experience Summary**: If 'description' text exists in input that isn't a bullet point, treat it as summary.
                    - **Keyword Strictness**:
                      * **STRICTLY EXCLUDE** all locations, city names, country names, and states (e.g., "Chicago", "London", "Remote", "India", "USA", "New York").
                      * **STRICTLY EXCLUDE** generic words (e.g., "Professional", "Senior", "Junior", "Experience", "Various").
                      * **STRICTLY EXCLUDE** dates, years, email addresses, and phone numbers.
                      * **Norm**: Return keywords in Title Case or Lowercase consistently.
                      * **Focus**: Only extract Technologies, Tools, Hard Skills (e.g. Java, AWS), and Specific Soft Skills (e.g. Leadership).
                    - **Formatting**:
                      * **NO MARKDOWN**: The 'improved' text must be plain text. Do NOT use **bold** or *italics*. It breaks the PDF generator.
        
                    **Resume Text**:
                    %s
        
                    **Job Description**:
                    %s
        
                    **OUTPUT FORMAT**:
                    Return ONLY a raw JSON object (no markdown). Use this exact structure:
                    {
                      "analysis": {
                        "matchScore": <0-100>,
                        "matchKeywords": ["string"],
                        "jdKeywords": ["string"],
                        "missingKeywords": ["string"],
                        "addedKeywords": ["string"],
                        "strengths": ["string"]
                      },
                      "suggestions": [
                        {
                          "id": "unique-id",
                          "type": "content",
                          "originalText": "string",
                          "suggestedText": "string",
                          "reason": "string",
                          "priority": "high"
                        }
                      ],
                      "resumeData": {
                        "personalInfo": {
                          "name": "string",
                          "phone": "string",
                          "email": "string",
                          "linkedin": "url or empty",
                          "portfolio": "url or empty"
                        },
                        "education": [
                          { "school": "string", "date": "string", "degree": "string", "gpa": "string" }
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
                        ]
                      }
                    }
                    """,
                truncatedResume, truncatedJD);
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

    private AnalysisResponse parseResponse(String llmOutput, String resumeText) {
        try {
            String cleanJson = extractJsonBlock(llmOutput);
            AnalysisResponse partialResponse = objectMapper.readValue(cleanJson, AnalysisResponse.class);

            partialResponse.setResumeText(resumeText);

            if (partialResponse.getAnalysis() != null) {
                partialResponse.setScore(partialResponse.getAnalysis().getMatchScore());
            }

            sanitizeResponse(partialResponse);

            return partialResponse;
        } catch (Exception e) {
            System.err.println("LLM Output that failed parsing: " + llmOutput);
            throw new RuntimeException("Failed to parse LLM JSON output", e);
        }
    }

    private void sanitizeResponse(AnalysisResponse response) {
        if (response.getResumeData() == null)
            return;

        if (response.getResumeData().getExperience() != null) {
            response.getResumeData().getExperience().forEach(exp -> {
                if (exp.getBulletPoints() != null) {
                    exp.getBulletPoints().forEach(bp -> {
                        if (bp.getImproved() != null) {
                            bp.setImproved(bp.getImproved().replace("**", "").replace("*", ""));
                        }
                    });
                }
            });
        }

        if (response.getResumeData().getProjects() != null) {
            response.getResumeData().getProjects().forEach(proj -> {
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

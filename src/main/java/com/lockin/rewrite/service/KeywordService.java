package com.lockin.rewrite.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class KeywordService {

    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "a", "an", "the", "and", "or", "but", "if", "then", "else", "when",
            "at", "by", "for", "from", "in", "into", "of", "off", "on", "onto",
            "out", "over", "to", "up", "with", "about", "against", "between",
            "through", "during", "before", "after", "above", "below", "under",
            "again", "further", "once", "here", "there", "where", "why", "how",
            "all", "any", "both", "each", "few", "more", "most", "other", "some",
            "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too",
            "very", "s", "t", "can", "will", "just", "don", "should", "now", "are",
            "is", "was", "were", "be", "been", "being", "have", "has", "had",
            "having", "do", "does", "did", "doing", "i", "we", "you", "he", "she",
            "it", "they", "them", "their", "theirs", "my", "your", "yours", "his",
            "her", "hers", "its", "our", "ours", "yourself", "yourselves", "himself",
            "herself", "itself", "themselves", "what", "which", "who", "whom",
            "this", "that", "these", "those", "am", "senior", "junior", "lead",
            "developer", "manager", "engineer", "description", "requirements",
            "preferred", "qualifications", "experience", "skills", "ability",
            "knowledge", "understanding", "proficient", "strong", "excellent",
            "good", "great", "working", "environment", "team", "player",
            "communication", "verbal", "written", "interpersonal", "organizational",
            "detail", "oriented", "highly", "motivated", "self", "starter",
            "job", "role", "position", "title", "responsibilities", "duties",
            "year", "years", "degree", "bachelor", "master", "phd", "diploma",
            "computer", "science", "engineering", "related", "field", "equivalent",
            "plus", "advantage", "bonus", "nice", "have", "must", "required",
            "looking", "seeking", "candidate", "applicant", "work", "time", "full",
            "part", "contract", "remote", "hybrid", "onsite", "office", "location",
            "salary", "benefits", "competitive", "package", "opportunity", "growth",
            "career", "path", "culture", "company", "business", "client", "customer",
            "service", "product", "project", "management", "development", "software",
            "application", "system", "solution", "technical", "technology", "tool",
            "language", "framework", "library", "database", "platform", "cloud",
            "web", "mobile", "ios", "android", "frontend", "backend", "fullstack",
            "devops", "agile", "scrum", "waterfall", "methodology", "lifecycle",
            "sdlc", "testing", "quality", "assurance", "control", "continuous",
            "integration", "deployment", "delivery", "pipeline", "automation",
            "manual", "unit", "integration", "system", "acceptance", "performance",
            "security", "scalability", "reliability", "availability", "efficiency",
            "optimization", "maintenance", "support", "documentation", "report",
            "analysis", "design", "implementation", "coding", "programming",
            "debugging", "troubleshooting", "resolution", "collaboration",
            "meeting", "stakeholder", "requirement", "specification", "user",
            "story", "case", "scenario", "diagram", "flowchart", "wireframe",
            "mockup", "prototype"));

    public List<String> extractKeywords(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        String normalized = text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ") // merge multiple spaces
                .trim();

        String[] tokens = normalized.split(" ");

        return Arrays.stream(tokens)
                .filter(token -> token.length() > 2) // Filter very short words
                .filter(token -> !STOPWORDS.contains(token))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public double calculateMatchScore(List<String> resumeKeywords, List<String> jdKeywords) {
        if (jdKeywords == null || jdKeywords.isEmpty()) {
            return 0.0;
        }

        long matchCount = jdKeywords.stream()
                .filter(resumeKeywords::contains)
                .count();

        double score = (double) matchCount / jdKeywords.size() * 100.0;

        return Math.round(score * 100.0) / 100.0;
    }

    public List<String> findMissingKeywords(List<String> resumeKeywords, List<String> jdKeywords) {
        if (jdKeywords == null || jdKeywords.isEmpty()) {
            return Collections.emptyList();
        }

        return jdKeywords.stream()
                .filter(keyword -> !resumeKeywords.contains(keyword))
                .sorted()
                .collect(Collectors.toList());
    }
}

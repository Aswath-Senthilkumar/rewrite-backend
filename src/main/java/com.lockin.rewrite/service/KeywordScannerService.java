package com.lockin.rewrite.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class KeywordScannerService {

    private final KeywordDictionary keywordDictionary;

    public KeywordScannerService(KeywordDictionary keywordDictionary) {
        this.keywordDictionary = keywordDictionary;
    }

    public KeywordAnalysisResult analyze(String jdText, String resumeText) {
        Set<String> foundInJD = scanText(jdText);
        Set<String> foundInResume = scanText(resumeText);

        // Calculate missing keywords (Present in JD but NOT in Resume)
        List<String> missingFromResume = foundInJD.stream()
                .filter(keyword -> !foundInResume.contains(keyword))
                .collect(Collectors.toList());

        return new KeywordAnalysisResult(
                new ArrayList<>(foundInJD),
                new ArrayList<>(foundInResume),
                missingFromResume
        );
    }

    private Set<String> scanText(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> foundKeywords = new HashSet<>();
        String lowerCaseText = text.toLowerCase();

        for (String keyword : keywordDictionary.getAllKeywords()) {
            List<String> aliases = keywordDictionary.getAliases(keyword);
            
            for (String alias : aliases) {
                // Use word boundaries to avoid partial matches (e.g., "Java" inside "JavaScript")
                // Escape special regex characters in the alias
                String escapedAlias = Pattern.quote(alias.toLowerCase());
                
                // For terms like "C++", "C#", ".NET", \b fails.
                // We can use a custom boundary check or just simple containment for now as a robust fallback
                // A good compromise:
                // If the alias contains special chars, use simple contains.
                // If it's purely alphanumeric, use \b.
                
                boolean isAlphanumeric = alias.matches("[a-zA-Z0-9\\s]+");
                
                if (isAlphanumeric) {
                     Pattern pattern = Pattern.compile("\\b" + escapedAlias + "\\b");
                     if (pattern.matcher(lowerCaseText).find()) {
                         foundKeywords.add(keyword); // Add the canonical keyword
                         break; // Found one alias, no need to check others for this keyword
                     }
                } else {
                    // For "C++", ".NET", etc.
                    if (lowerCaseText.contains(alias.toLowerCase())) {
                        foundKeywords.add(keyword);
                        break;
                    }
                }
            }
        }
        return foundKeywords;
    }

    public static class KeywordAnalysisResult {
        private final List<String> foundInJD;
        private final List<String> foundInResume;
        private final List<String> missingFromResume;

        public KeywordAnalysisResult(List<String> foundInJD, List<String> foundInResume, List<String> missingFromResume) {
            this.foundInJD = foundInJD;
            this.foundInResume = foundInResume;
            this.missingFromResume = missingFromResume;
        }

        public List<String> getFoundInJD() {
            return foundInJD;
        }

        public List<String> getFoundInResume() {
            return foundInResume;
        }

        public List<String> getMissingFromResume() {
            return missingFromResume;
        }
    }
}

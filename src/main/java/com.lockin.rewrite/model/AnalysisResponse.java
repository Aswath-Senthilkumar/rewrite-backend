package com.lockin.rewrite.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisResponse {

    private String resumeText;
    private Analysis analysis;
    private List<Suggestion> suggestions;
    private double score;
    private ResumeData resumeData;
}

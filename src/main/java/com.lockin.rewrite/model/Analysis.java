package com.lockin.rewrite.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Analysis {

    private double matchScore;
    private List<String> strengths;
    private List<String> missingKeywords;

    private List<String> matchKeywords;
    private List<String> jdKeywords;

    private List<String> addedKeywords;
}

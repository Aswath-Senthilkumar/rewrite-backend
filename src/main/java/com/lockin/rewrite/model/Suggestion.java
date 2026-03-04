package com.lockin.rewrite.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Suggestion {

    private String id;
    private String type;
    private String originalText;
    private String suggestedText;
    private int startIndex;
    private int endIndex;
    private String reason;
    private String priority;
}

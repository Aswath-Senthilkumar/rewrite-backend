package com.lockin.rewrite.model.resume;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BulletPoints {
    private String original;
    private String improved;
    private boolean accepted;
    private List<String> injectedKeywords;
}

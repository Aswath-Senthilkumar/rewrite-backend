package com.lockin.rewrite.model.resume;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BulletPoints {
    private String original;
    private String improved;
    private boolean accepted;
}

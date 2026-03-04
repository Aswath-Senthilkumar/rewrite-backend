package com.lockin.rewrite.model.resume;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Experience {
    private String title;
    private String company;
    private String date;
    private String location;
    private String summary;
    private List<BulletPoints> bulletPoints;
}

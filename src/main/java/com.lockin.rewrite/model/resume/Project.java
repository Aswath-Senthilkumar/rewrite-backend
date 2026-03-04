package com.lockin.rewrite.model.resume;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Project {

    private String title;
    private String link;
    private String date;
    private String summary;
    private String location;
    private List<BulletPoints> bulletPoints;
}

package com.lockin.rewrite.model;

import com.lockin.rewrite.model.resume.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResumeData {

    private PersonalInfo personalInfo;
    private String summary;
    private List<Education> education;
    private Skills skills;
    private List<Experience> experience;
    private List<Project> projects;
    private List<Certification> certifications;
    private List<Award> awards;
}

package com.lockin.rewrite.service;


import com.lockin.rewrite.model.ResumeData;
import com.lockin.rewrite.model.resume.*;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

@Service
public class LatexService {

    public byte[] generatePdf(ResumeData data) throws IOException, InterruptedException {
        // 1. Generate LaTeX String
        String latexContent = buildLatex(data);

        // 2. Write to temp file
        File tempDir = Files.createTempDirectory("resume_gen").toFile();
        File texFile = new File(tempDir, "resume.tex");
        try (PrintWriter out = new PrintWriter(texFile)) {
            out.println(latexContent);
        }

        // 3. Compile with Tectonic
        ProcessBuilder pb = new ProcessBuilder("tectonic", "resume.tex");
        pb.directory(tempDir);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Capture output for debugging
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("[Tectonic] " + line);
        }

        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished || process.exitValue() != 0) {
            throw new RuntimeException("Latex compilation failed");
        }

        // 4. Read PDF
        File pdfFile = new File(tempDir, "resume.pdf");
        if (!pdfFile.exists()) {
            throw new RuntimeException("PDF file not generated");
        }
        return Files.readAllBytes(pdfFile.toPath());
    }

    private String buildLatex(ResumeData data) {
        // Use the raw template structure and inject data
        // For simplicity in this first pass, we construct the document using the
        // structure from the provided template

        StringBuilder sb = new StringBuilder();
        // Header (Imports & Config)
        sb.append("\\documentclass[letterpaper,10pt]{article}\n");
        sb.append("\\usepackage{latexsym}\n");
        sb.append("\\usepackage[empty]{fullpage}\n");
        sb.append("\\usepackage{titlesec}\n");
        sb.append("\\usepackage{marvosym}\n");
        sb.append("\\usepackage[usenames,dvipsnames]{color}\n");
        sb.append("\\usepackage{verbatim}\n");
        sb.append("\\usepackage{enumitem}\n");
        sb.append("\\usepackage[colorlinks=true, urlcolor=blue, linkcolor=red]{hyperref}\n");
        sb.append("\\usepackage{fancyhdr}\n");
        sb.append("\\usepackage{babel}\n");
        sb.append("\\usepackage{tabularx}\n");
        sb.append("\\usepackage{fontawesome5}\n");
        sb.append("\\usepackage{multicol}\n");
        sb.append("\\setlength{\\multicolsep}{-3.0pt}\n");
        sb.append("\\setlength{\\columnsep}{-1pt}\n");
        sb.append("\\setlist[itemize]{nosep, topsep=10pt, partopsep=1pt, parsep=1pt}\n");
        // sb.append("\\input{glyphtounicode}\n"); // Tectonic/XeTeX doesn't need this
        // Margins
        sb.append("\\addtolength{\\oddsidemargin}{-0.6in}\n");
        sb.append("\\addtolength{\\evensidemargin}{-0.5in}\n");
        sb.append("\\addtolength{\\textwidth}{1.19in}\n");
        sb.append("\\addtolength{\\topmargin}{-.7in}\n");
        sb.append("\\addtolength{\\textheight}{1.4in}\n");
        sb.append("\\urlstyle{same}\n");
        sb.append("\\raggedbottom\n");
        sb.append("\\raggedright\n");
        sb.append("\\setlength{\\tabcolsep}{0in}\n");
        sb.append(
                "\\titleformat{\\section}{\\vspace{-2pt}\\scshape\\raggedright\\normalsize\\selectfont}{}{0em}{}[\\color{black}\\titlerule \\vspace{-3pt}]\n");
        // sb.append("\\pdfgentounicode=1\n"); // Tectonic/XeTeX doesn't need this
        // Commands
        sb.append("\\newcommand{\\resumeItem}[1]{\\item\\small{{#1 \\vspace{-2pt}}}}\n");
        sb.append(
                "\\newcommand{\\resumeSubheading}[4]{\\vspace{-4pt}\\item\\begin{tabular*}{1.0\\textwidth}[t]{l@{\\extracolsep{\\fill}}r}\\textbf{#1} & \\textbf{\\small #2} \\\\[-1pt] \\textit{\\small#3} & \\textit{\\small #4} \\\\ \\end{tabular*}\\vspace{-7pt}}\n");
        sb.append(
                "\\newcommand{\\resumeProjectHeading}[2]{\\item\\begin{tabular*}{1.001\\textwidth}{l@{\\extracolsep{\\fill}}r}\\small#1 & \\textbf{\\small #2}\\\\ \\end{tabular*}\\vspace{-10pt}}\n");
        sb.append("\\newcommand{\\resumeSubHeadingListStart}{\\begin{itemize}[leftmargin=0.0in, label={}]}\n");
        sb.append("\\newcommand{\\resumeSubHeadingListEnd}{\\end{itemize}}\n");
        sb.append("\\newcommand{\\resumeItemListStart}{\\begin{itemize}}\n");
        sb.append("\\newcommand{\\resumeItemListEnd}{\\end{itemize}\\vspace{-5pt}}\n");
        sb.append("\\begin{document}\n");

        // Personal Info
        PersonalInfo info = data.getPersonalInfo();
        if (info != null) {
            sb.append("\\begin{center}\n");
            sb.append("    {\\huge \\scshape ").append(escape(info.getName())).append("} \\\\ \\vspace{1pt}\n");
            sb.append("    \\small \\raisebox{-0.1\\height}\\faPhone\\ ").append(escape(info.getPhone()))
                    .append(" ~\n");
            sb.append("    \\href{mailto:").append(info.getEmail())
                    .append("}{\\raisebox{-0.2\\height}\\faEnvelope\\  \\underline{").append(escape(info.getEmail()))
                    .append("}} ~\n");
            if (info.getLinkedin() != null && !info.getLinkedin().isEmpty()) {
                sb.append("    \\href{").append(info.getLinkedin())
                        .append("}{\\raisebox{-0.2\\height}\\faLinkedin\\ \\underline{linkedin}} ~\n");
            }
            if (info.getWebsite() != null && !info.getWebsite().isEmpty()) {
                sb.append("    \\href{").append(info.getWebsite())
                        .append("}{\\raisebox{-0.2\\height}\\Mundus\\ \\underline{portfolio}}\n");
            }
            sb.append("\\end{center}\n");
            sb.append("\\vspace{-20pt}\n");
        }

        // Professional Summary
        if (data.getSummary() != null && !data.getSummary().isEmpty()) {
            sb.append("\\section{PROFESSIONAL SUMMARY}\n");
            sb.append("\\small{").append(escape(data.getSummary())).append("}\n");
            sb.append("\\vspace{-10pt}\n");
        }

        // Education
        if (data.getEducation() != null && !data.getEducation().isEmpty()) {
            sb.append("\\section{EDUCATION}\n");
            sb.append("\\resumeSubHeadingListStart\n");
            for (Education edu : data.getEducation()) {
                sb.append("  \\resumeSubheading\n");
                sb.append("    {").append(escape(edu.getInstitution())).append("}{").append(escape(edu.getDate()))
                        .append("}\n");
                sb.append("    {").append(escape(edu.getDegree())).append("}{").append(escape(edu.getGpa()))
                        .append("}\n");
                sb.append("    \\vspace{5pt}\n");
            }
            sb.append("\\resumeSubHeadingListEnd\n");
            sb.append("\\vspace{-26pt}\n");
        }

        // Skills
        if (data.getSkills() != null) {
            sb.append("\\section{SKILLS}\n");
            sb.append("\\begin{itemize}[leftmargin=0.15in, label={}]\n");
            sb.append("\\vspace{-2pt}\n");
            sb.append("\\small{\\item{\n");
            if (data.getSkills().getLanguages() != null)
                sb.append("\\textbf{Languages}{: ").append(escape(data.getSkills().getLanguages())).append("} \\\\\n");
            if (data.getSkills().getFrameworks() != null)
                sb.append("\\textbf{Frameworks}{: ").append(escape(data.getSkills().getFrameworks()))
                        .append("} \\\\\n");
            if (data.getSkills().getTools() != null)
                sb.append("\\textbf{Tools}{: ").append(escape(data.getSkills().getTools())).append("} \\\\\n");
            sb.append("}}\n");
            sb.append("\\vspace{-2pt}\n");
            sb.append("\\end{itemize}\n");
            sb.append("\\vspace{-20pt}\n"); // FROM TEMPLATE
        }

        // Experience
        if (data.getExperience() != null && !data.getExperience().isEmpty()) {
            sb.append("\\section{INDUSTRIAL EXPERIENCE}\n");
            sb.append("\\resumeSubHeadingListStart\n");
            for (int i = 0; i < data.getExperience().size(); i++) {
                Experience exp = data.getExperience().get(i);

                sb.append("  \\resumeSubheading\n");
                sb.append("    {").append(escape(exp.getTitle())).append(" -- ").append(escape(exp.getCompany()))
                        .append("}{").append(escape(exp.getDate())).append("}\n");
                sb.append("    {").append(escape(exp.getSummary())).append("}{").append(escape(exp.getLocation()))
                        .append("}\n");
                sb.append("    \\vspace{-10pt}\n");
                sb.append("    \\resumeItemListStart\n");
                if (exp.getBulletPoints() != null) {
                    for (BulletPoints bp : exp.getBulletPoints()) {
                        String text = bp.isAccepted() ? bp.getImproved() : bp.getOriginal();
                        sb.append("      \\resumeItem{").append(escape(text)).append("}\n");
                    }
                }
                sb.append("    \\resumeItemListEnd\n");

                // Add spacing between experience items as per template example
                if (i < data.getExperience().size() - 1) {
                    sb.append("\\vspace{1pt}\n"); // FROM TEMPLATE
                }
            }
            sb.append("\\resumeSubHeadingListEnd\n");
            sb.append("\\vspace{-26pt}\n"); // FROM TEMPLATE
        }

        // Projects
        if (data.getProjects() != null && !data.getProjects().isEmpty()) {
            sb.append("\\section{PROJECTS}\n");
            // sb.append("\\vspace{-4pt}\n"); // FROM TEMPLATE
            sb.append("\\resumeSubHeadingListStart\n");
            for (int i = 0; i < data.getProjects().size(); i++) {
                Project proj = data.getProjects().get(i);

                System.out.println("Processing Project: " + proj.getTitle() + ", Date: " + proj.getDate());

                // If we have detailed info (summary or location), use the Subheading format
                boolean hasExtraInfo = (proj.getSummary() != null && !proj.getSummary().isEmpty()) ||
                        (proj.getLocation() != null && !proj.getLocation().isEmpty());

                if (hasExtraInfo) {
                    sb.append("  \\resumeSubheading\n");
                    sb.append("    {\\textbf{").append(escape(proj.getTitle())).append("}}{")
                            .append(escape(proj.getDate())).append("}\n");
                    sb.append("    {").append(escape(proj.getSummary())).append("}{").append(escape(proj.getLocation()))
                            .append("}\n");
                    sb.append("    \\vspace{-3pt}\n"); // Adjusted spacing
                } else {
                    sb.append("  \\resumeProjectHeading\n");
                    sb.append("    {\\textbf{").append(escape(proj.getTitle())).append("}}{")
                            .append(escape(proj.getDate())).append("}\n");
                }

                sb.append("    \\vspace{-7pt}\n"); // Adjusted spacing
                sb.append("    \\resumeItemListStart\n");
                if (proj.getBulletPoints() != null) {
                    for (BulletPoints bp : proj.getBulletPoints()) {
                        String text = bp.isAccepted() ? bp.getImproved() : bp.getOriginal();
                        sb.append("      \\resumeItem{").append(escape(text)).append("}\n");
                    }
                }
                sb.append("    \\resumeItemListEnd\n");

                if (i < data.getProjects().size() - 1) {
                    sb.append("\\vspace{0pt}\n");
                }
            }
            sb.append("\\resumeSubHeadingListEnd\n");
        }

        // Certifications
        if (data.getCertifications() != null && !data.getCertifications().isEmpty()) {
            sb.append("\\section{CERTIFICATIONS}\n");
            sb.append("\\resumeSubHeadingListStart\n");
            for (Certification cert : data.getCertifications()) {
                sb.append("  \\resumeSubheading\n");
                sb.append("    {").append(escape(cert.getName())).append("}{").append(escape(cert.getDate())).append("}\n");
                sb.append("    {").append(escape(cert.getIssuer())).append("}{}\n");
                sb.append("    \\vspace{5pt}\n");
            }
            sb.append("\\resumeSubHeadingListEnd\n");
            sb.append("\\vspace{-26pt}\n");
        }

        // Awards
        if (data.getAwards() != null && !data.getAwards().isEmpty()) {
            sb.append("\\section{AWARDS}\n");
            sb.append("\\resumeSubHeadingListStart\n");
            for (Award award : data.getAwards()) {
                sb.append("  \\resumeSubheading\n");
                sb.append("    {").append(escape(award.getTitle())).append("}{").append(escape(award.getDate())).append("}\n");
                sb.append("    {").append(escape(award.getIssuer())).append("}{}\n");
                sb.append("    \\vspace{5pt}\n");
            }
            sb.append("\\resumeSubHeadingListEnd\n");
            sb.append("\\vspace{-26pt}\n");
        }

        sb.append("\\end{document}\n");
        return sb.toString();
    }

    private String escape(String input) {
        if (input == null)
            return "";
        return input.replace("&", "\\&")
                .replace("%", "\\%")
                .replace("$", "\\$")
                .replace("#", "\\#")
                .replace("_", "\\_")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("~", "\\textasciitilde ")
                .replace("^", "\\textasciicircum ");
    }
}

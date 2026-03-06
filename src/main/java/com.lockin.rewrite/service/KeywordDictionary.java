package com.lockin.rewrite.service;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class KeywordDictionary {

    private final Map<String, List<String>> dictionary;

    public KeywordDictionary() {
        this.dictionary = new HashMap<>();
        initializeDictionary();
    }

    private void initializeDictionary() {
        // Languages
        add("Java", "Java 8", "Java 11", "Java 17", "J2EE", "Jakarta EE");
        add("Python", "Python 3", "Py3");
        add("JavaScript", "JS", "ES6", "ECMAScript");
        add("TypeScript", "TS");
        add("C++", "Cpp", "C Plus Plus");
        add("C#", "CSharp", ".NET Core");
        add("Go", "Golang");
        add("Rust", "RustLang");
        add("Swift", "SwiftUI");
        add("Kotlin", "Kotlin Android");
        add("Ruby", "Ruby on Rails", "RoR");
        add("PHP", "Laravel", "Symfony");
        add("SQL", "Structured Query Language", "PL/SQL", "T-SQL");

        // Frontend
        add("React", "React.js", "ReactJS", "React Native");
        add("Angular", "AngularJS", "Angular 2+");
        add("Vue", "Vue.js", "VueJS");
        add("Next.js", "NextJS", "Next");
        add("HTML", "HTML5");
        add("CSS", "CSS3", "Sass", "SCSS", "Less");
        add("Tailwind", "Tailwind CSS");
        add("Bootstrap", "Bootstrap 4", "Bootstrap 5");

        // Backend & Frameworks
        add("Spring Boot", "SpringBoot", "Spring Framework", "Spring MVC");
        add("Node.js", "NodeJS", "Node", "Express.js");
        add("Django", "Django REST Framework", "DRF");
        add("Flask", "Flask Python");
        add("FastAPI", "Fast API");
        add("GraphQL", "Apollo GraphQL");

        // Databases
        add("PostgreSQL", "Postgres", "PGSQL");
        add("MySQL", "My SQL");
        add("MongoDB", "Mongo", "NoSQL");
        add("Redis", "Redis Cache");
        add("Cassandra", "Apache Cassandra");
        add("Elasticsearch", "Elastic Search", "ELK Stack");
        add("DynamoDB", "AWS DynamoDB");

        // Cloud & DevOps
        add("AWS", "Amazon Web Services", "EC2", "S3", "Lambda");
        add("Azure", "Microsoft Azure");
        add("GCP", "Google Cloud Platform", "Google Cloud");
        add("Docker", "Docker Container", "Dockerfile");
        add("Kubernetes", "K8s", "Kube");
        add("Jenkins", "Jenkins CI/CD");
        add("Git", "GitHub", "GitLab", "Bitbucket");
        add("Terraform", "HashiCorp Terraform", "IaC");
        add("Ansible", "Red Hat Ansible");
        add("CircleCI", "Circle CI");

        // Concepts & Methodologies
        add("Agile", "Scrum", "Kanban");
        add("Microservices", "Micro-services", "Distributed Systems");
        add("REST API", "RESTful API", "Rest API");
        add("CI/CD", "Continuous Integration", "Continuous Deployment");
        add("TDD", "Test Driven Development");
        add("System Design", "High Level Design", "Low Level Design");
    }

    private void add(String key, String... aliases) {
        List<String> list = new ArrayList<>();
        list.add(key); // The key itself is a valid term
        Collections.addAll(list, aliases);
        dictionary.put(key, list);
    }

    public Set<String> getAllKeywords() {
        return dictionary.keySet();
    }

    public List<String> getAliases(String keyword) {
        return dictionary.getOrDefault(keyword, Collections.emptyList());
    }
}

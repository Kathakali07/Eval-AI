package com.evalai.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("Concept")
public class ConceptNode {

    @Id
    private String name;

    @Property("subject_area")
    private String subjectArea;

    public ConceptNode() {
    }

    public ConceptNode(String subjectArea, String name) {
        this.subjectArea = subjectArea;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubjectArea() {
        return subjectArea;
    }

    public void setSubjectArea(String subjectArea) {
        this.subjectArea = subjectArea;
    }
}

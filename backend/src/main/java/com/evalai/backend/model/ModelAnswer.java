package com.evalai.backend.model;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "ModelAnswers")
public class ModelAnswer {

    @Id
    private String id;

    @Field("subject")
    private String subject;

    @Field("question_number")
    private String questionNumber;

    @Field("question_text")
    private String question;

    @Field("model_answer")
    private String answer;

    @Field("max_marks")
    private Double maxMarks;

    @Field("vector_embedding")
    private List<Double> vectorEmbedding;
    
    @Field("contains_math")
    private Boolean containsMath;
    
    @Field("has_diagram")
    private Boolean hasDiagram;
    
    @Field("diagram_snippet")
    private String diagramSnippet;


    public ModelAnswer() {
    }

    public ModelAnswer(String subject, String questionNumber, String question, String answer, Double maxMarks, List<Double> vectorEmbedding, Boolean containsMath, Boolean hasDiagram, String diagramSnippet) {
        this.subject = subject;
        this.questionNumber = questionNumber;
        this.question = question;
        this.answer = answer;
        this.maxMarks = maxMarks;
        this.vectorEmbedding = vectorEmbedding;
        this.containsMath = containsMath;
        this.hasDiagram = hasDiagram;
        this.diagramSnippet = diagramSnippet;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getQuestionNumber() { return questionNumber; }
    public void setQuestionNumber(String questionNumber) { this.questionNumber = questionNumber; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public Double getMaxMarks() { return maxMarks; }
    public void setMaxMarks(Double maxMarks) { this.maxMarks = maxMarks; }

    public List<Double> getVectorEmbedding() { return vectorEmbedding; }
    public void setVectorEmbedding(List<Double> vectorEmbedding) { this.vectorEmbedding = vectorEmbedding; }
    
    public Boolean getContainsMath() { return containsMath; }
    public void setContainsMath(Boolean containsMath) { this.containsMath = containsMath; }
    
    public Boolean getHasDiagram() { return hasDiagram; }
    public void setHasDiagram(Boolean hasDiagram) { this.hasDiagram = hasDiagram; }
    
    public String getDiagramSnippet() { return diagramSnippet; }
    public void setDiagramSnippet(String diagramSnippet) { this.diagramSnippet = diagramSnippet; }
}

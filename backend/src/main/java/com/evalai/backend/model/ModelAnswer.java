package com.evalai.backend.model;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "ModelAnswers")
public class ModelAnswer {

    @Id
            private String id;
    private String subject;
    private String question;
    private String answer;
    private List<Double> vectorEmbedding;

    public ModelAnswer() {
    }

    public ModelAnswer(String id, String subject, String question, String answer, List<Double> vectorEmbedding) {
        this.id = id;
        this.subject = subject;
        this.question = question;
        this.answer = answer;
        this.vectorEmbedding = vectorEmbedding;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<Double> getVectorEmbedding() {
        return vectorEmbedding;
    }

    public void setVectorEmbedding(List<Double> vectorEmbedding) {
        this.vectorEmbedding = vectorEmbedding;
    }
}

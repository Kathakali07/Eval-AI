package com.evalai.backend.service;

import com.evalai.backend.dto.*;
import com.evalai.backend.model.ModelAnswer;
import com.evalai.backend.repository.ModelAnswerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class EvaluationOrchestrator {

        private final RestClient restClient;
        private final ModelAnswerRepository mongoRepository;
        private final Neo4jClient neo4jClient;
        private final ObjectMapper objectMapper;

        public EvaluationOrchestrator(ModelAnswerRepository mongoRepository,
                        Neo4jClient neo4jClient) {
                this.mongoRepository = mongoRepository;
                this.neo4jClient = neo4jClient;
                this.objectMapper = new ObjectMapper();
                this.restClient = RestClient.builder()
                                .baseUrl("http://127.0.0.1:8000")
                                .build();
        }

        /**
         * Evaluates a full student exam: OCR the script, match each answer to the
         * model,
         * grade via vector similarity + knowledge graph, and return a consolidated
         * report card.
         */
        public ReportCardDTO processAndGradeStudentUpload(String subjectArea, MultipartFile studentFile) {
                try {
                        // Step 1: OCR the student handwriting
                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", studentFile.getResource());
                        body.add("document_type", "student");

                        PythonOcrResponse ocrResponse = restClient.post()
                                        .uri("/read")
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .body(body)
                                        .retrieve()
                                        .body(PythonOcrResponse.class);

                        if (ocrResponse == null || ocrResponse.structured_data() == null) {
                                throw new RuntimeException("OCR extraction failed to return structured data.");
                        }

                        StudentExtractionDTO studentExtraction = objectMapper.readValue(
                                        ocrResponse.structured_data(),
                                        StudentExtractionDTO.class);

                        // Step 2: Fetch the knowledge graph for this subject
                        String cypherQuery = "MATCH (sub:Concept {subject_area: $subjectArea})-[rel]->(obj:Concept) " +
                                        "RETURN sub.name AS subject, type(rel) AS predicate, obj.name AS object";

                        List<TripletDTO> modelTriplets = new ArrayList<>();
                        neo4jClient.query(cypherQuery)
                                        .bind(subjectArea).to("subjectArea")
                                        .fetch()
                                        .all()
                                        .forEach(record -> modelTriplets.add(new TripletDTO(
                                                        (String) record.get("subject"),
                                                        (String) record.get("predicate"),
                                                        (String) record.get("object"))));

                        // Step 3: Grade each student answer
                        double totalScore = 0.0;
                        double totalMaxMarks = 0.0;
                        List<GradedQuestionDTO> gradedQuestions = new ArrayList<>();

                        for (StudentAnswerItemDTO studentAnswer : studentExtraction.answers()) {

                                ModelAnswer targetAnswer = mongoRepository.findBySubjectAndQuestionNumber(
                                                subjectArea,
                                                studentAnswer.question_number());

                                if (targetAnswer == null) {
                                        continue; // No model answer key for this question
                                }

                                PythonEvalRequest evalRequest = new PythonEvalRequest(
                                                studentAnswer.student_text(),
                                                targetAnswer.getVectorEmbedding(),
                                                modelTriplets);

                                PythonEvalResponse pythonGrade = restClient.post()
                                                .uri("/evaluate/grade-answer")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .body(evalRequest)
                                                .retrieve()
                                                .body(PythonEvalResponse.class);

                                double achievedMarks = pythonGrade.semantic_score() * targetAnswer.getMaxMarks();
                                totalScore += achievedMarks;
                                totalMaxMarks += targetAnswer.getMaxMarks();

                                gradedQuestions.add(new GradedQuestionDTO(
                                                studentAnswer.question_number(),
                                                Math.round(achievedMarks * 100.0) / 100.0,
                                                targetAnswer.getMaxMarks(),
                                                pythonGrade.missing_concepts(),
                                                pythonGrade.feedback()));
                                try {
                                        System.out.println("   [Rate Limit Defense] Pausing for 15 seconds to respect API Free Tier limit...");
                                        Thread.sleep(15000);
                                } catch (InterruptedException e) {
                                        // Good practice to re-interrupt the thread if it's broken
                                        Thread.currentThread().interrupt();
                                        System.err.println("Thread sleep interrupted: " + e.getMessage());
                                }
                        }

                        return new ReportCardDTO(subjectArea, Math.round(totalScore * 100.0) / 100.0, totalMaxMarks,
                                        gradedQuestions);

                } catch (Exception e) {
                        System.err.println("Evaluation pipeline failed: " + e.getMessage());
                        throw new RuntimeException("Student evaluation failed", e);
                }
        }
}

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
    private final ObjectMapper objectMapper; // Spring's JSON parser

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
     * THE MASTER EVALUATION PIPELINE: Full Student Script -> Loop -> Grade -> Report Card
     */
    public ReportCardDTO processAndGradeStudentUpload(String subjectArea, MultipartFile studentFile) {

        System.out.println("\n=== STARTING FULL EXAM EVALUATION ===");

        try {
            // STEP 1: OCR - Extract the handwriting into our structured Student JSON Array
            System.out.println("1. Forwarding file to Python Vision AI (Student Mode)...");
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", studentFile.getResource());
            body.add("document_type", "student"); // Crucial: Tells Python to use the Student schema

            PythonOcrResponse ocrResponse = restClient.post()
                    .uri("/read") // Match your updated read_api.py endpoint
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(PythonOcrResponse.class);

            if (ocrResponse == null || ocrResponse.structured_data() == null) {
                throw new RuntimeException("OCR extraction failed to return structured data.");
            }

            // Parse the stringified JSON array into our Java DTO
            StudentExtractionDTO studentExtraction = objectMapper.readValue(
                    ocrResponse.structured_data(),
                    StudentExtractionDTO.class
            );
            System.out.println("   Extracted " + studentExtraction.answers().size() + " handwritten answers.");

            // STEP 2: Pre-Fetch the Global Brain from Neo4j ONCE (Optimization)
            System.out.println("2. Fetching Expanded Knowledge Graph for '" + subjectArea + "'...");
            String cypherQuery =
                    "MATCH (sub:Concept {subject_area: $subjectArea})-[rel]->(obj:Concept) " +
                            "RETURN sub.name AS subject, type(rel) AS predicate, obj.name AS object";

            List<TripletDTO> modelTriplets = new ArrayList<>();
            neo4jClient.query(cypherQuery)
                    .bind(subjectArea).to("subjectArea")
                    .fetch()
                    .all()
                    .forEach(record -> modelTriplets.add(new TripletDTO(
                            (String) record.get("subject"),
                            (String) record.get("predicate"),
                            (String) record.get("object")
                    )));

            // STEP 3: The Evaluation Loop
            System.out.println("3. Beginning Grading Loop...");
            double totalScore = 0.0;
            double totalMaxMarks = 0.0;
            List<GradedQuestionDTO> gradedQuestions = new ArrayList<>();

            for (StudentAnswerItemDTO studentAnswer : studentExtraction.answers()) {
                System.out.println("\n   -> Grading Question " + studentAnswer.question_number() + "...");

                // 3a. Find the exact matching question in MongoDB
                ModelAnswer targetAnswer = mongoRepository.findBySubjectAndQuestionNumber(
                        subjectArea,
                        studentAnswer.question_number()
                );

                if (targetAnswer == null) {
                    System.out.println("      [WARNING] Question " + studentAnswer.question_number() + " not found in Model Paper. Skipping.");
                    continue; // Skip grading if the teacher didn't provide an answer key for this
                }

                // 3b. Send the text, the specific math vector, and the graph to Python
                PythonEvalRequest evalRequest = new PythonEvalRequest(
                        studentAnswer.student_text(),
                        targetAnswer.getVectorEmbedding(),
                        modelTriplets
                );

                PythonEvalResponse pythonGrade = restClient.post()
                        .uri("/evaluate/grade-answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(evalRequest)
                        .retrieve()
                        .body(PythonEvalResponse.class);

                // 3c. Calculate the physical marks (e.g., 0.8 AI score * 5 max marks = 4 marks)
                double achievedMarks = pythonGrade.semantic_score() * targetAnswer.getMaxMarks();

                // Add to our running totals
                totalScore += achievedMarks;
                totalMaxMarks += targetAnswer.getMaxMarks();

                // 3d. Add the detailed result to our Report Card
                gradedQuestions.add(new GradedQuestionDTO(
                        studentAnswer.question_number(),
                        Math.round(achievedMarks * 100.0) / 100.0, // Round to 2 decimals
                        targetAnswer.getMaxMarks(),
                        pythonGrade.missing_concepts(),
                        pythonGrade.feedback()
                ));
            }

            // STEP 4: Return the master Report Card to the Controller
            System.out.println("\n=== EVALUATION COMPLETE: Scored " + totalScore + " / " + totalMaxMarks + " ===");
            return new ReportCardDTO(subjectArea, Math.round(totalScore * 100.0) / 100.0, totalMaxMarks, gradedQuestions);

        } catch (Exception e) {
            System.err.println("Pipeline failed: " + e.getMessage());
            throw new RuntimeException("Student Evaluation failed", e);
        }
    }
}

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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
         * model, grade via the holistic evaluator, and return a consolidated report card.
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

                        // Confidence check — hard-reject if the AI couldn't read the handwriting clearly
                        if (ocrResponse.confidence_score() != null && ocrResponse.confidence_score() < 0.7) {
                                throw new RuntimeException(String.format(
                                                "The student's paper could not be read clearly enough (confidence: %.0f%%, minimum required: 70%%). "
                                                                + "Please rescan the document with better lighting or higher resolution.",
                                                ocrResponse.confidence_score() * 100));
                        }

                        StudentExtractionDTO studentExtraction = objectMapper.readValue(
                                        ocrResponse.structured_data(),
                                        StudentExtractionDTO.class);

                        // Step 2: Grade each student answer via the holistic evaluator
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

                                // Fetch the specific knowledge graph concept for THIS question from Neo4j
                                String cypherQuery = "MATCH (sub:Concept {subject_area: $subjectArea, question_number: $questionNumber})-[rel]->(obj:Concept) " +
                                                "RETURN sub.name AS subject, type(rel) AS predicate, obj.name AS object";

                                List<TripletDTO> modelTriplets = new ArrayList<>();
                                neo4jClient.query(cypherQuery)
                                                .bind(subjectArea).to("subjectArea")
                                                .bind(studentAnswer.question_number()).to("questionNumber")
                                                .fetch()
                                                .all()
                                                .forEach(record -> modelTriplets.add(new TripletDTO(
                                                                (String) record.get("subject"),
                                                                (String) record.get("predicate"),
                                                                (String) record.get("object"))));

                                // Build teacher_facts string from triplets mapped specifically to this question
                                String teacherFacts = buildTeacherFacts(modelTriplets);

                                // Build the new holistic eval request
                                PythonEvalRequest evalRequest = new PythonEvalRequest(
                                                studentAnswer.question_number(),
                                                studentAnswer.student_text(),
                                                teacherFacts,
                                                targetAnswer.getDiagramSnippet(), // teacher_diagram_rubric
                                                studentAnswer.contains_math() != null ? studentAnswer.contains_math() : false,
                                                studentAnswer.has_diagram() != null ? studentAnswer.has_diagram() : false,
                                                studentAnswer.diagram_snippet()   // student diagram image base64
                                );

                                PythonEvalResponse pythonGrade = restClient.post()
                                                .uri("/evaluate/eval")
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
                        }

                        return new ReportCardDTO(subjectArea, Math.round(totalScore * 100.0) / 100.0, totalMaxMarks,
                                        gradedQuestions);

                } catch (ResourceAccessException e) {
                        System.err.println("AI Engine unreachable: " + e.getMessage());
                        throw new RuntimeException(
                                        "The AI processing engine is not running. Please start the Python AI service and try again.");
                } catch (HttpClientErrorException e) {
                        System.err.println("AI Engine client error: " + e.getResponseBodyAsString());
                        String detail = e.getResponseBodyAsString();
                        throw new RuntimeException(detail.isEmpty()
                                        ? "The AI engine rejected the request (HTTP " + e.getStatusCode().value() + ")."
                                        : detail);
                } catch (HttpServerErrorException e) {
                        System.err.println("AI Engine server error: " + e.getResponseBodyAsString());
                        String detail = e.getResponseBodyAsString();
                        throw new RuntimeException(detail.isEmpty()
                                        ? "The AI engine encountered an internal error. Please try again."
                                        : detail);
                } catch (RuntimeException e) {
                        throw e;
                } catch (Exception e) {
                        System.err.println("Evaluation pipeline failed: " + e.getMessage());
                        throw new RuntimeException("An unexpected error occurred during evaluation: " + e.getMessage());
                }
        }

        /**
         * Builds a teacher_facts string from Neo4j triplets.
         * If the __MATH__ sentinel is present, returns the raw LaTeX equation.
         * Otherwise, serializes triplets as "subject predicate object" statements.
         */
        private String buildTeacherFacts(List<TripletDTO> triplets) {
                // Check for the __MATH__ sentinel triplet (set by data_ingest.py math bypass)
                for (TripletDTO t : triplets) {
                        if ("__MATH__".equals(t.subject())) {
                                // Return the raw LaTeX equation stored in the object field
                                return t.object();
                        }
                }

                // Standard text path: serialize triplets as readable statements
                if (triplets.isEmpty()) {
                        return "No reference facts available.";
                }

                return triplets.stream()
                                .map(t -> t.subject() + " " + t.predicate() + " " + t.object())
                                .collect(Collectors.joining("; "));
        }
}

package com.evalai.backend.service;

import com.evalai.backend.dto.*;
import com.evalai.backend.model.ModelAnswer;
import com.evalai.backend.repository.ModelAnswerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Service
public class IngestionOrchestrator {

    private final RestClient restClient;
    private final ModelAnswerRepository mongoRepository;
    private final KnowledgeGraphService neo4jService;
    private final ObjectMapper objectMapper;

    public IngestionOrchestrator(ModelAnswerRepository mongoRepository,
            KnowledgeGraphService neo4jService) {
        this.mongoRepository = mongoRepository;
        this.neo4jService = neo4jService;
        this.objectMapper = new ObjectMapper();
        this.restClient = RestClient.builder()
                .baseUrl("http://127.0.0.1:8000")
                .build();
    }

    /**
     * Full ingestion pipeline: OCR the teacher paper, vectorize each Q&A, and store
     * in MongoDB + Neo4j.
     */
    public void processFullTeacherPaper(String subjectArea, MultipartFile modelPaperFile) {
        try {
            // Step 1: OCR extraction
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", modelPaperFile.getResource());
            body.add("document_type", "teacher");

            PythonOcrResponse ocrResponse = restClient.post()
                    .uri("/read")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(PythonOcrResponse.class);

            if (ocrResponse == null || ocrResponse.structured_data() == null) {
                throw new RuntimeException("OCR extraction failed.");
            }

            // Confidence check — hard-reject if the AI couldn't read the document clearly
            if (ocrResponse.confidence_score() != null && ocrResponse.confidence_score() < 0.7) {
                throw new RuntimeException(String.format(
                        "The teacher's paper could not be read clearly enough (confidence: %.0f%%, minimum required: 70%%). "
                                + "Please rescan the document with better lighting or higher resolution.",
                        ocrResponse.confidence_score() * 100));
            }

            // Step 2: Parse structured data
            TeacherExtractionDTO extraction = objectMapper.readValue(
                    ocrResponse.structured_data(),
                    TeacherExtractionDTO.class);

            // Step 3: Process each question
            for (QAPairDTO qa : extraction.qa_pairs()) {

                // Vectorize the model answer
                PythonIngestResponse aiResponse = restClient.post()
                        .uri("/ingest/process-text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new PythonIngestRequest(qa.model_answer()))
                        .retrieve()
                        .body(PythonIngestResponse.class);

                // Persist to MongoDB
                ModelAnswer savedAnswer = new ModelAnswer(
                        subjectArea,
                        qa.question_number(),
                        qa.question_text(),
                        qa.model_answer(),
                        qa.max_marks() != null ? qa.max_marks() : 5.0,
                        aiResponse.vector_embedding(),
                        qa.contains_math() != null ? qa.contains_math() : false,
                        qa.has_diagram() != null ? qa.has_diagram() : false,
                        qa.diagram_snippet()
                );
                mongoRepository.save(savedAnswer);

                // Insert knowledge graph triplets into Neo4j
                for (TripletDTO triplet : aiResponse.triplets()) {
                    neo4jService.insertTriplet(
                            triplet.subject(),
                            triplet.predicate(),
                            triplet.object(),
                            subjectArea);
                }
            }

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
            System.err.println("Ingestion pipeline failed: " + e.getMessage());
            throw new RuntimeException("An unexpected error occurred during ingestion: " + e.getMessage());
        }
    }
}
package com.evalai.backend.service;

import com.evalai.backend.dto.*;
import com.evalai.backend.model.ModelAnswer;
import com.evalai.backend.repository.ModelAnswerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
                        aiResponse.vector_embedding());
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

        } catch (Exception e) {
            System.err.println("Ingestion pipeline failed: " + e.getMessage());
            throw new RuntimeException("Ingestion failed", e);
        }
    }
}
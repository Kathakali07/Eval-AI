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
    private final ObjectMapper objectMapper; // Spring's built-in JSON parser

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
     * THE MASTER INGESTION PIPELINE: Image -> OCR Array -> Loop -> Vectors/Graphs
     */
    public void processFullTeacherPaper(String subjectArea, MultipartFile modelPaperFile) {

        System.out.println("--- STARTING FULL EXAM INGESTION ---");

        try {
            // STEP 1: OCR - Extract the entire paper into structured JSON
            System.out.println("1. Sending exam paper to Vision AI...");
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", modelPaperFile.getResource());
            body.add("document_type", "teacher"); // Tells Python to use the Teacher schema

            PythonOcrResponse ocrResponse = restClient.post()
                    .uri("/read") // Make sure this matches your read_api.py router
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(PythonOcrResponse.class);

            if (ocrResponse == null || ocrResponse.structured_data() == null) {
                throw new RuntimeException("OCR extraction failed.");
            }

            // STEP 2: Parse the stringified JSON into our Java DTO
            TeacherExtractionDTO extraction = objectMapper.readValue(
                    ocrResponse.structured_data(),
                    TeacherExtractionDTO.class
            );

            System.out.println("2. Extracted " + extraction.qa_pairs().size() + " questions. Starting ingestion loop...");

            // STEP 3: Loop through EVERY question in the exam paper
            for (QAPairDTO qa : extraction.qa_pairs()) {
                System.out.println("\n--- Processing Question " + qa.question_number() + " ---");

                // 3a. Call Python to convert the teacher's answer into math vectors and graph triplets
                PythonIngestResponse aiResponse = restClient.post()
                        .uri("/ingest/process-text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new PythonIngestRequest(qa.model_answer()))
                        .retrieve()
                        .body(PythonIngestResponse.class);

                // 3b. Save to MongoDB using our new fields (Question Number & Max Marks)
                ModelAnswer savedAnswer = new ModelAnswer(
                        subjectArea,
                        qa.question_number(),
                        qa.question_text(),
                        qa.model_answer(),
                        qa.max_marks() != null ? qa.max_marks() : 5.0, // Default to 5 if AI misses it
                        aiResponse.vector_embedding()
                );
                mongoRepository.save(savedAnswer);
                System.out.println("   -> Saved Vector to MongoDB.");

                // 3c. Weave the facts into the Neo4j Global Brain
                for (TripletDTO triplet : aiResponse.triplets()) {
                    neo4jService.insertTriplet(
                            triplet.subject(),
                            triplet.predicate(),
                            triplet.object(),
                            subjectArea
                    );
                }
                System.out.println("   -> Merged facts into Neo4j.");
            }

            System.out.println("\n--- INGESTION COMPLETE ---");

        } catch (Exception e) {
            System.err.println("Pipeline failed: " + e.getMessage());
            throw new RuntimeException("Ingestion failed", e);
        }
    }
}
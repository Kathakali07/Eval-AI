package com.evalai.backend.controller;

import com.evalai.backend.service.IngestionOrchestrator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ingestion")
@CrossOrigin(origins = "*")
public class IngestionController {

    private final IngestionOrchestrator ingestionOrchestrator;

    public IngestionController(IngestionOrchestrator ingestionOrchestrator) {
        this.ingestionOrchestrator = ingestionOrchestrator;
    }

    /**
     * Accepts a teacher's model answer paper and ingests it into the knowledge
     * base.
     */
    @PostMapping(value = "/upload-teacher-paper", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadModelPaper(
            @RequestParam("file") MultipartFile modelPaperFile,
            @RequestParam("subjectArea") String subjectArea) {
        try {
            ingestionOrchestrator.processFullTeacherPaper(subjectArea, modelPaperFile);
            return ResponseEntity.ok().body("{\"status\": \"success\"}");
        } catch (Exception e) {
            System.err.println("Ingestion failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
        }
    }
}
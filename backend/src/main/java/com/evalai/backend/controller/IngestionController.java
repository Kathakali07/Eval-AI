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

        if (modelPaperFile.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("{\"status\": \"error\", \"message\": \"No file was uploaded. Please select a file and try again.\"}");
        }

        if (subjectArea == null || subjectArea.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("{\"status\": \"error\", \"message\": \"Subject area is required. Please enter a subject and try again.\"}");
        }

        try {
            ingestionOrchestrator.processFullTeacherPaper(subjectArea.trim(), modelPaperFile);
            return ResponseEntity.ok().body("{\"status\": \"success\"}");
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "An unknown error occurred.";
            System.err.println("Ingestion failed: " + msg);

            // Determine HTTP status based on error message content
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            if (msg.contains("not running") || msg.contains("not configured")) {
                status = HttpStatus.SERVICE_UNAVAILABLE;
            } else if (msg.contains("rate limit")) {
                status = HttpStatus.TOO_MANY_REQUESTS;
            } else if (msg.contains("could not be read clearly") || msg.contains("rescan")) {
                status = HttpStatus.valueOf(422);
            }

            return ResponseEntity.status(status)
                    .body("{\"status\": \"error\", \"message\": \"" + msg.replace("\"", "'") + "\"}");
        }
    }
}
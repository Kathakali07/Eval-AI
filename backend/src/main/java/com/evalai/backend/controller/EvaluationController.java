package com.evalai.backend.controller;

import com.evalai.backend.dto.ReportCardDTO;
import com.evalai.backend.service.EvaluationOrchestrator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/evaluate")
@CrossOrigin(origins = "*")
public class EvaluationController {

    private final EvaluationOrchestrator evaluationOrchestrator;

    public EvaluationController(EvaluationOrchestrator evaluationOrchestrator) {
        this.evaluationOrchestrator = evaluationOrchestrator;
    }

    /**
     * Grades a student's handwritten exam against the ingested model answers for
     * the given subject.
     */
    @PostMapping(value = "/grade-student", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> gradeStudent(
            @RequestParam("file") MultipartFile studentFile,
            @RequestParam("subjectArea") String subjectArea) {

        if (studentFile.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("{\"status\": \"error\", \"message\": \"No file was uploaded. Please select a file and try again.\"}");
        }

        if (subjectArea == null || subjectArea.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("{\"status\": \"error\", \"message\": \"Subject area is required. Please enter a subject and try again.\"}");
        }

        try {
            ReportCardDTO reportCard = evaluationOrchestrator.processAndGradeStudentUpload(
                    subjectArea.trim(),
                    studentFile);

            return ResponseEntity.ok(reportCard);

        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "An unknown error occurred.";
            System.err.println("Evaluation failed: " + msg);

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
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"status\": \"error\", \"message\": \"An unexpected error occurred processing the exam. Please try again.\"}");
        }
    }
}
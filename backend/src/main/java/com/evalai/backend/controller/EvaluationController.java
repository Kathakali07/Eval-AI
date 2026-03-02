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

        try {
            if (studentFile.isEmpty()) {
                return ResponseEntity.badRequest().body("Error: No file uploaded.");
            }

            ReportCardDTO reportCard = evaluationOrchestrator.processAndGradeStudentUpload(
                    subjectArea,
                    studentFile);

            return ResponseEntity.ok(reportCard);

        } catch (RuntimeException e) {
            System.err.println("Evaluation failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Pipeline Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred processing the exam.");
        }
    }
}
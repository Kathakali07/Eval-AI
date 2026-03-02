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
@CrossOrigin(origins = "*") // Crucial for your React frontend to avoid CORS errors
public class EvaluationController {

    private final EvaluationOrchestrator evaluationOrchestrator;

    public EvaluationController(EvaluationOrchestrator evaluationOrchestrator) {
        this.evaluationOrchestrator = evaluationOrchestrator;
    }

    /**
     * THE FULL DOCUMENT GRADING ENDPOINT
     * React hits this endpoint with ONLY the student's handwritten image and the subject.
     * The AI handles mapping the questions automatically.
     */
    @PostMapping(value = "/grade-student", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> gradeStudent(
            @RequestParam("file") MultipartFile studentFile,
            @RequestParam("subjectArea") String subjectArea) {

        try {
            if (studentFile.isEmpty()) {
                return ResponseEntity.badRequest().body("Error: No file uploaded.");
            }

            System.out.println("\n--- API Gateway: Received grading request for subject: " + subjectArea + " ---");

            // Pass the raw file and subject directly to the master orchestrator.
            // It now returns a complete Report Card for the entire exam.
            ReportCardDTO finalReportCard = evaluationOrchestrator.processAndGradeStudentUpload(
                    subjectArea,
                    studentFile
            );

            // Return the final JSON Report Card to React
            return ResponseEntity.ok(finalReportCard);

        } catch (RuntimeException e) {
            System.err.println("Evaluation Pipeline Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Pipeline Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected Server Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred processing the exam.");
        }
    }
}